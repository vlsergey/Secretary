package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.mutable.MutableInt;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.cache.wikidata.SitelinksCache;
import org.wikipedia.vlsergey.secretary.dom.AbstractContainer;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityProperty;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Rank;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.StringValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikibaseEntityIdValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class MoveDataToWikidataWorker {

	private static final EntityId ITEM_HUMAN = EntityId.item(5);

	private static final EntityId ITEM_RUWIKI = EntityId.item(206855);

	private static final Logger log = LoggerFactory.getLogger(MoveDataToWikidata.class);

	private static final Collection<EntityId> PROHIBITED_TYPES = Arrays.asList(EntityId.item(14756018),
			EntityId.item(14073567), EntityId.item(15052790), EntityId.item(1141470), EntityId.item(31184),
			EntityId.item(16334295), EntityId.item(281643), EntityId.item(215380), EntityId.item(10648343),
			EntityId.item(164950), EntityId.item(1156073), EntityId.item(13417114), EntityId.item(8436),
			EntityId.item(15618652), EntityId.item(16979650), EntityId.item(4167410), EntityId.item(4167836),
			EntityId.item(13406463), EntityId.item(4), EntityId.item(132821), EntityId.item(4164871),
			EntityId.item(8261), EntityId.item(386724), EntityId.item(571), EntityId.item(1371849),
			EntityId.item(273057)

	);

	static Content prepareParameterValue(Content content) {
		if (content instanceof AbstractContainer) {
			AbstractContainer container = (AbstractContainer) content;
			final List<? extends Content> originalChildren = container.getChildren();
			List<Content> children = new ArrayList<>(originalChildren.size());
			for (Content child : originalChildren) {
				if (child instanceof Template) {
					Template template = (Template) child;
					if (StringUtils.equalsIgnoreCase("s", template.getName().toWiki(true))
							&& template.getParameters().size() == 1) {
						children.add(template.getParameter(0));
					} else {
						children.add(template);
					}
				} else {
					children.add(prepareParameterValue(child));
				}
			}
			return new ArticleFragment(children);
		}
		return content;
	}

	private TreeMap<String, MutableInt> errorsCounter = new TreeMap<>();

	@Autowired
	@Qualifier("ruWikipediaBot")
	private MediaWikiBot ruWikipediaBot;

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	@Autowired
	private SitelinksCache sitelinksCache;

	@Autowired
	private WikidataBot wikidataBot;

	void countException(UnsupportedParameterValueException exc) {
		String message = exc.getMessage();

		String wikiMessage = "<nowiki>" + message + "</nowiki>";
		if (exc.getEntityId() != null) {
			wikiMessage += " <span class='entity-link' data-entity-id='" + exc.getEntityId() + "'>("
					+ exc.getEntityId().toWikilink(true) + ")</span>";
		}

		synchronized (this) {
			if (!errorsCounter.containsKey(wikiMessage)) {
				errorsCounter.put(wikiMessage, new MutableInt(0));
			}
			errorsCounter.get(wikiMessage).increment();
		}
	}

	void errorsReportClear() {
		synchronized (this) {
			errorsCounter.clear();
		}
	}

	void errorsReportDump() {
		List<String> allMessages = new ArrayList<>(errorsCounter.keySet());
		Collections.sort(allMessages, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				return -Integer.compare(errorsCounter.get(o1).intValue(), errorsCounter.get(o2).intValue());
			}
		});

		StringBuilder stringBuilder = new StringBuilder();
		int count = 0;
		for (String errorMessage : allMessages) {

			stringBuilder.append("# ");
			stringBuilder.append(errorsCounter.get(errorMessage).intValue());
			stringBuilder.append(": ");
			stringBuilder.append(errorMessage);
			stringBuilder.append("\n");
			count++;
			if (count == 100) {
				break;
			}
		}

		ruWikipediaCache.getMediaWikiBot().writeContent(
				"User:" + ruWikipediaCache.getMediaWikiBot().getLogin() + "/Top100Errors", null,
				stringBuilder.toString(), null, "Update reconsiliation stats", true, false);
	}

	private void fillFromWikipedia(Template template, ReconsiliationColumn descriptor,
			Collection<ValueWithQualifiers> result) {
		for (String templateParameter : descriptor.templateParameters) {
			for (TemplatePart part : template.getParameters(templateParameter)) {
				final Content parameterValue = part.getValue();
				if (parameterValue != null) {
					try {
						String value = StringUtils.trimToEmpty(prepareParameterValue(parameterValue).toWiki(true));
						if (StringUtils.isNotBlank(value)) {
							result.addAll(descriptor.parseF.apply(value));
						}
					} catch (UnsupportedParameterValueException exc) {
						exc.setTemplatePartValue(parameterValue);
						countException(exc);
						throw exc;
					}
				}
			}
		}
	}

	private String generateWikidataSummary(EntityId templateId,
			Map<ReconsiliationColumn, ? extends Collection<ValueWithQualifiers>> fromPedia) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Transferring fields of [[");
		stringBuilder.append(templateId.toString());
		stringBuilder.append("]] from ruwiki: ");
		for (Map.Entry<ReconsiliationColumn, ? extends Collection<ValueWithQualifiers>> entry : fromPedia.entrySet()) {
			Object[] properties = Arrays.stream(entry.getKey().properties).map(x -> "[[" + x.getPageTitle() + "]]")
					.toArray();
			stringBuilder.append(StringUtils.join(properties, ", "));
			stringBuilder.append(": ");
			for (ValueWithQualifiers valueWithQualifiers : entry.getValue()) {
				if (valueWithQualifiers.getValue().getSnakType() != SnakType.value) {
					stringBuilder.append("(" + valueWithQualifiers.getValue().getSnakType() + "); ");
				} else {
					DataValue value = valueWithQualifiers.getValue().getDataValue();
					if (value instanceof StringValue) {
						stringBuilder.append("«");
						stringBuilder.append(((StringValue) value).getValue());
						stringBuilder.append("»; ");
					} else if (value instanceof WikibaseEntityIdValue) {
						stringBuilder.append(((WikibaseEntityIdValue) value).getEntityId().toWikilink(false));
						stringBuilder.append("; ");
					} else {
						stringBuilder.append(value.toWiki(new Locale("en"), x -> x.toString()).toWiki(true));
						stringBuilder.append("; ");
					}
				}
			}
		}
		return stringBuilder.toString();
	}

	private void process(EntityByLinkResolver entityByLinkResolver, TitleResolver titleResolver, String TEMPLATE,
			EntityId templateId, ReconsiliationColumn[] parametersToMove, Revision revision, Entity entity,
			MoveDataReport report) throws Exception {

		Map<ReconsiliationColumn, List<ValueWithQualifiers>> fromPedia = new LinkedHashMap<>();

		ArticleFragment fragment = ruWikipediaBot.getXmlParser().parse(revision);
		if (fragment.getTemplates(TEMPLATE.toLowerCase()).isEmpty()) {
			return;
		}

		for (ReconsiliationColumn descriptor : parametersToMove) {
			try {
				List<ValueWithQualifiers> fromPediaSet = new ArrayList<>();
				for (Template template : fragment.getAllTemplates().get(TEMPLATE.toLowerCase())) {
					fillFromWikipedia(template, descriptor, fromPediaSet);
				}
				fromPedia.put(descriptor, fromPediaSet);
			} catch (UnsupportedParameterValueException exc) {
				report.addLine(revision, descriptor, exc);
				continue;
			}
		}

		boolean allCollectionsAreEmptry = fromPedia.values().stream().allMatch(collection -> collection.isEmpty());
		if (allCollectionsAreEmptry) {
			// nothing to move
			return;
		}

		if (entity != null) {
			titleResolver.update(entity);
		}

		if (entity == null) {
			JSONObject data = new JSONObject();

			{
				final JSONObject labels = new JSONObject();
				labels.put("language", "ru");
				labels.put("value", revision.getPage().getTitle());
				data.put("labels", Collections.singletonMap("ru", labels));
			}
			{
				final JSONObject sitelink = new JSONObject();
				sitelink.put("site", "ruwiki");
				sitelink.put("title", revision.getPage().getTitle());
				data.put("sitelinks", Collections.singletonMap("ruwiki", sitelink));
			}
			Entity.putProperty(data, Statement.newStatement(Properties.INSTANCE_OF, ITEM_HUMAN));

			entity = wikidataBot.wgCreateEntity(data, "Precreate item for transferring fields");
			entity = wikidataBot.wgGetEntityBySitelink("ruwiki", revision.getPage().getTitle(), EntityProperty.claims);
			if (entity == null) {
				throw new RuntimeException();
			}
			titleResolver.update(entity);

			// return;
		} else {
			// check compatibility
			for (Statement statement : entity.getClaims(Properties.INSTANCE_OF)) {
				if (statement.hasValue()) {
					EntityId instanceOf = EntityId.item(statement.getMainSnak().getWikibaseEntityIdValue()
							.getNumericId());
					if (PROHIBITED_TYPES.contains(instanceOf)) {
						for (ReconsiliationColumn descriptor : parametersToMove) {
							report.addLine(revision, descriptor,
									"Unsupported entity type: [[:d:" + instanceOf.toString() + "]]", entity);
						}
						return;
					}
				}
			}
		}

		final List<String> claimIdsToDelete = new ArrayList<>();
		final JSONObject newData = new JSONObject();
		boolean needWikipediaSave = false;

		for (ReconsiliationColumn descriptor : parametersToMove) {
			List<ValueWithQualifiers> fromWikipedia = fromPedia.get(descriptor);
			Map<EntityId, List<String>> toBeDeleted = new HashMap<>();

			if (fromWikipedia == null) {
				// skip, not parsed
				continue;
			}

			List<ValueWithQualifiers> fromWikidata = descriptor.fromWikidata(entity, false);
			ReconsiliationAction action = descriptor.getAction(fromWikipedia, fromWikidata);

			if (action == ReconsiliationAction.report_difference) {
				final List<ValueWithQualifiers> fromWikidataWithoutImported = descriptor.fromWikidata(entity, true);
				final ReconsiliationAction actionWithoutImported = descriptor.getAction(fromWikipedia,
						fromWikidataWithoutImported);

				if (actionWithoutImported != ReconsiliationAction.report_difference) {
					for (EntityId property : descriptor.properties) {
						final List<String> toBeDeletedByProperty = new LinkedList<>(entity.getClaims(property).stream()
								.filter(x -> x.isImportedFrom(ITEM_RUWIKI)).map(x -> x.getId())
								.collect(Collectors.toList()));
						toBeDeleted.put(property, toBeDeletedByProperty);
					}
					fromWikidata = fromWikidataWithoutImported;
					action = actionWithoutImported;
				}
			}

			switch (action) {
			case replace: {
				for (EntityId property : descriptor.properties) {
					List<String> toBeDeletedByProperty = toBeDeleted.get(property);
					if (toBeDeletedByProperty == null) {
						toBeDeletedByProperty = new LinkedList<>();
						toBeDeleted.put(property, toBeDeletedByProperty);
					}

					for (Statement statement : entity.getClaims(property)) {
						if (statement.hasRealReferences()) {
							statement.setRank(Rank.deprecated);
							Entity.putProperty(newData, statement);
						} else {
							toBeDeletedByProperty.add(statement.getId());
						}
					}
				}
			}
			case append:
				descriptor.fillToWikidata(fromWikipedia, newData, toBeDeleted);
				break;
			case remove_from_wikipedia_as_empty:
			case remove_from_wikipedia_as_not_empty:
				fromPedia.remove(descriptor);
				break;
			case report_difference:
				report.addLine(revision, descriptor, fromWikipedia, fromWikidata, entity);
				fromPedia.remove(descriptor);
				continue;
			default:
				throw new UnsupportedOperationException("NYI");
			}

			claimIdsToDelete.addAll(toBeDeleted.values().stream().flatMap(x -> x.stream()).collect(Collectors.toSet()));
			if (action.removeFromWikipedia) {
				for (Template template : fragment.getAllTemplates().get(TEMPLATE.toLowerCase())) {
					for (String templateParameter : descriptor.templateParameters) {
						if (!template.getParameters(templateParameter).isEmpty()) {
							template.removeParameter(templateParameter);
							if (action != ReconsiliationAction.remove_from_wikipedia_as_empty) {
								needWikipediaSave = true;
							}
						}
					}
				}
			}
		}

		final String summary = generateWikidataSummary(templateId, fromPedia);
		if (newData.length() != 0) {
			wikidataBot.wgEditEntity(entity, newData, summary);
		}
		if (!claimIdsToDelete.isEmpty()) {
			wikidataBot.wgRemoveClaims(entity, claimIdsToDelete.toArray(new String[claimIdsToDelete.size()]), summary);
		}

		if (needWikipediaSave) {
			ruWikipediaBot.writeContent(revision, fragment.toWiki(false), "Перенос параметров [[Шаблон:" + TEMPLATE
					+ "]] на [[Викиданные]]", true);
		}
	}

	public void process(EntityByLinkResolver entityByLinkResolver, TitleResolver titleResolver, String template,
			SinglePropertyReconsiliationColumn... columns) {
		EntityId templateId = entityByLinkResolver.apply("Шаблон:" + template).getId();
		MoveDataReport report = new MoveDataReport(new Locale("ru"), titleResolver);

		final Iterable<Revision> revisions = ruWikipediaCache.queryByEmbeddedIn("Шаблон:" + template,
				Namespace.NSS_MAIN);
		final Iterable<Map.Entry<Revision, Entity>> revisionsWithEntity = sitelinksCache.getWithEntitiesF(
				Project.RUWIKIPEDIA).apply(revisions);

		for (Map.Entry<Revision, Entity> revisionAndEntity : revisionsWithEntity) {
			try {
				process(entityByLinkResolver, titleResolver, template, templateId, columns, revisionAndEntity.getKey(),
						revisionAndEntity.getValue(), report);
			} catch (Exception exc) {
				log.error(exc.toString(), exc);
				// throw new RuntimeException(e);
			}
		}

		report.save(template, ruWikipediaBot, columns);
	}
}
