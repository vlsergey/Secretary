package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiReference;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiSnak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiStatement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataValue;
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

	private static ApiReference REFERENCE_FROM_RUWIKI = new ApiReference();

	static {
		REFERENCE_FROM_RUWIKI.addSnak(ApiSnak.newSnak(ApiStatement.PROPERTY_IMPORTED_FROM, ITEM_RUWIKI));
	}

	@Autowired
	@Qualifier("ruWikipediaBot")
	private MediaWikiBot ruWikipediaBot;

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	@Autowired
	private WikidataBot wikidataBot;

	private void fillFromWikidata(ApiEntity entity, EntityId property, Collection<ValueWithQualifiers> result) {
		for (ApiStatement statement : entity.getClaims(property)) {
			result.add(new ValueWithQualifiers(statement));
		}
	}

	private void fillFromWikipedia(Template template, ReconsiliationColumn descriptor,
			Collection<ValueWithQualifiers> result) {
		for (String templateParameter : descriptor.templateParameters) {
			for (TemplatePart part : template.getParameters(templateParameter)) {
				final Content parameterValue = part.getValue();
				if (parameterValue != null) {
					try {
						String value;
						if (parameterValue instanceof Template
								&& StringUtils
										.equalsIgnoreCase("s", ((Template) parameterValue).getName().toWiki(true))) {
							value = ((Template) parameterValue).getParameter(0).getValue().toWiki(true);
						} else {
							value = parameterValue.toWiki(true).trim();
						}

						if (StringUtils.isNotBlank(value)) {
							result.addAll(descriptor.toWikidata.apply(value));
						}
					} catch (UnsupportedParameterValueException exc) {
						exc.setTemplatePartValue(parameterValue);
						throw exc;
					}
				}
			}
		}
	}

	private void fillToWikidata(ReconsiliationColumn descriptor, Collection<ValueWithQualifiers> source,
			JSONObject result) {
		for (ValueWithQualifiers newValue : source) {
			ApiStatement statement = ApiStatement.newStatement(descriptor.property, newValue.getValue());
			// add qualifiers
			for (ApiSnak qualifier : newValue.getQualifiers()) {
				statement.addQualifier(qualifier);
			}
			statement.addReference(REFERENCE_FROM_RUWIKI);
			ApiEntity.putProperty(result, statement);
		}
	}

	private String generateSummary(EntityId templateId,
			Map<ReconsiliationColumn, ? extends Collection<ValueWithQualifiers>> fromPedia) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Transferring fields of [[");
		stringBuilder.append(templateId.toString());
		stringBuilder.append("]] from ruwiki: ");
		for (Map.Entry<ReconsiliationColumn, ? extends Collection<ValueWithQualifiers>> entry : fromPedia.entrySet()) {
			stringBuilder.append("[[Property:" + entry.getKey().property + "]]: ");
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
						stringBuilder.append(value.toWiki(x -> x.toString()).toWiki(true));
						stringBuilder.append("; ");
					}
				}
			}
		}
		return stringBuilder.toString();
	}

	private void process(EntityByLinkResolver entityByLinkResolver, TitleResolver titleResolver, String TEMPLATE,
			EntityId templateId, ReconsiliationColumn[] parametersToMove, Revision revision, MoveDataReport report)
			throws Exception {

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

		ApiEntity entity = entityByLinkResolver.apply(revision.getPage().getTitle());
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
			ApiEntity.putProperty(data, ApiStatement.newStatement(Properties.INSTANCE_OF, ITEM_HUMAN));

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

			if (fromWikipedia == null) {
				// skip, not parsed
				continue;
			}

			List<ValueWithQualifiers> fromWikidata = new ArrayList<>();
			fillFromWikidata(entity, descriptor.property, fromWikidata);
			ReconsiliationAction action = descriptor.getAction(fromWikipedia, fromWikidata);

			final List<ApiStatement> claims = Arrays.asList(entity.getClaims(descriptor.property));
			if (claims.stream().anyMatch(x -> x.isImportedFrom(ITEM_RUWIKI))) {
				if (action == ReconsiliationAction.report_difference) {
					claimIdsToDelete.addAll(claims.stream().filter(x -> x.isImportedFrom(ITEM_RUWIKI))
							.map(x -> x.getId()).collect(Collectors.toList()));
					fromWikidata = claims.stream().filter(x -> !x.isImportedFrom(ITEM_RUWIKI))
							.map(x -> new ValueWithQualifiers(x)).collect(Collectors.toList());
					action = descriptor.getAction(fromWikipedia, fromWikidata);
				}
			}

			switch (action) {
			case replace: {
				for (ApiStatement statement : entity.getClaims(descriptor.property)) {
					if (statement.hasRealReferences()) {
						statement.setRank(Rank.deprecated);
						ApiEntity.putProperty(newData, statement);
					} else {
						claimIdsToDelete.add(statement.getId());
					}
				}
				fillToWikidata(descriptor, fromWikipedia, newData);
				break;
			}
			case remove_from_wikipedia:
				fromPedia.remove(descriptor);
				break;
			case report_difference:
				report.addLine(revision, descriptor, fromWikipedia, fromWikidata, entity);
				fromPedia.remove(descriptor);
				continue;
			case set:
				fillToWikidata(descriptor, fromWikipedia, newData);
				break;
			default:
				throw new UnsupportedOperationException("NYI");
			}

			if (action.removeFromWikipedia) {
				for (Template template : fragment.getAllTemplates().get(TEMPLATE.toLowerCase())) {
					for (String templateParameter : descriptor.templateParameters) {
						if (!template.getParameters(templateParameter).isEmpty()) {
							template.removeParameter(templateParameter);
							needWikipediaSave = true;
						}
					}
				}
			}
		}

		final String summary = generateSummary(templateId, fromPedia);
		if (newData.length() != 0) {
			wikidataBot.wgEditEntity(entity, newData, summary);
		}
		if (!claimIdsToDelete.isEmpty()) {
			wikidataBot.wgRemoveClaims(entity, claimIdsToDelete.toArray(new String[claimIdsToDelete.size()]), summary);
		}

		if (needWikipediaSave) {
			ruWikipediaBot.writeContent(revision, fragment.toWiki(false), "Move [[Шаблон:" + TEMPLATE
					+ "]] parameters to Wikidata", true);
		}
	}

	public void process(EntityByLinkResolver entityByLinkResolver, TitleResolver titleResolver, String template,
			ReconsiliationColumn... columns) {
		EntityId templateId = entityByLinkResolver.apply("Шаблон:" + template).getId();
		MoveDataReport report = new MoveDataReport(titleResolver);

		ExecutorService executorService = Executors.newFixedThreadPool(4);
		List<Future<?>> tasks = new ArrayList<>();

		for (Revision revision : ruWikipediaCache.queryByEmbeddedIn("Шаблон:" + template, Namespace.NSS_MAIN)) {
			// for (Revision revision :
			// Collections.singletonList(ruWikipediaCache.queryLatestRevision("Альдрованди, Улиссе")))
			// {
			tasks.add(executorService.submit(new Runnable() {
				@Override
				public void run() {
					try {
						process(entityByLinkResolver, titleResolver, template, templateId, columns, revision, report);
					} catch (Exception exc) {
						log.error(exc.toString(), exc);
						// throw new RuntimeException(e);
					}
				}
			}));
		}

		for (Future<?> task : tasks) {
			try {
				task.get();
			} catch (Exception exc) {
				log.error(exc.toString(), exc);
			}
		}

		report.save(template, ruWikipediaBot, columns);
	}
}
