package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.cache.wikidata.SitelinksCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class CompareWithWikidataWorker {

	private static final EntityId ITEM_HUMAN = EntityId.item(5);

	private static final Logger log = LoggerFactory.getLogger(CompareWithWikidata.class);

	private static final Collection<EntityId> PROHIBITED_TYPES = Arrays.asList(EntityId.item(14756018),
			EntityId.item(14073567), EntityId.item(15052790), EntityId.item(1141470), EntityId.item(31184),
			EntityId.item(16334295), EntityId.item(281643), EntityId.item(215380), EntityId.item(10648343),
			EntityId.item(164950), EntityId.item(1156073), EntityId.item(13417114), EntityId.item(8436),
			EntityId.item(15618652), EntityId.item(16979650), EntityId.item(4167410), EntityId.item(4167836),
			EntityId.item(13406463), EntityId.item(4), EntityId.item(132821), EntityId.item(4164871),
			EntityId.item(8261), EntityId.item(386724), EntityId.item(571), EntityId.item(1371849),
			EntityId.item(273057)

	);

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

	private void process(EntityByLinkResolver entityByLinkResolver, TitleResolver titleResolver, String templateName,
			EntityId templateId, ReconsiliationColumn[] parametersToMove, Revision revision, Entity entity,
			MoveDataReport report) throws Exception {

		Map<ReconsiliationColumn, List<ValueWithQualifiers>> fromPedia = new LinkedHashMap<>();

		ArticleFragment fragment = ruWikipediaBot.getXmlParser().parse(revision);
		if (fragment.getTemplates(templateName.toLowerCase()).isEmpty()) {
			return;
		}
		if (fragment.getTemplates(templateName.toLowerCase()).size() != 1) {
			return;
		}
		Template template = fragment.getTemplates(templateName.toLowerCase()).get(0);

		for (ReconsiliationColumn column : parametersToMove) {
			try {
				fromPedia.put(column, column.fromWikipedia(template, exc -> countException(exc)));
			} catch (UnsupportedParameterValueException exc) {
				report.addLine(revision, column, exc);
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
			return;
		}

		// check compatibility
		for (Statement statement : entity.getClaims(Properties.INSTANCE_OF)) {
			if (statement.hasValue()) {
				EntityId instanceOf = EntityId.item(statement.getMainSnak().getWikibaseEntityIdValue().getNumericId());
				if (PROHIBITED_TYPES.contains(instanceOf)) {
					for (ReconsiliationColumn descriptor : parametersToMove) {
						report.addLine(revision, descriptor, "Unsupported entity type: [[:d:" + instanceOf.toString()
								+ "]]", entity);
					}
					return;
				}
			}
		}

		for (ReconsiliationColumn descriptor : parametersToMove) {
			List<ValueWithQualifiers> fromWikipedia = fromPedia.get(descriptor);

			if (fromWikipedia == null) {
				// skip, not parsed
				continue;
			}

			List<ValueWithQualifiers> fromWikidata = descriptor.fromWikidata(entity, false);
			final ReconsiliationAction action = descriptor.getAction(fromWikipedia, fromWikidata);

			switch (action) {
			case replace:
			case report_difference:
				report.addLine(revision, descriptor, fromWikipedia, fromWikidata, entity);
			}
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
