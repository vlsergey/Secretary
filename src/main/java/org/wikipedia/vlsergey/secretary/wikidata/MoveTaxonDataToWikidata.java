package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiStatement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityProperty;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class MoveTaxonDataToWikidata implements Runnable {

	private static final String NOVALUE = "(novalue)";

	private static final Set<String> NOVALUES = new HashSet<>(Arrays.asList(
			"notpl", "noipni"));

	private static final Map<String, EntityId> parametersToMove = new LinkedHashMap<>();

	private static final String TEMPLATE = "Таксон";

	static {
		parametersToMove.put("itis", EntityId.property(815));
		parametersToMove.put("ncbi", EntityId.property(685));
		parametersToMove.put("eol", EntityId.property(830));
		parametersToMove.put("ipni", EntityId.property(961));
		parametersToMove.put("tpl", EntityId.property(1070));
	}

	@Autowired
	@Qualifier("ruWikipediaBot")
	private MediaWikiBot ruWikipediaBot;

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	@Autowired
	private WikidataBot wikidataBot;

	private void fillFromWikidata(Entity entity, EntityId property,
			Set<String> result) {
		for (Statement statement : entity.getClaims(property)) {
			switch (statement.getMainSnak().getSnakType()) {
			case novalue:
				result.add(NOVALUE);
				continue;
			case somevalue:
				continue;
			case value:
				result.add(statement.getStringValue().getValue());
			}
		}
	}

	private void fillFromWikipedia(Template template, String parameterName,
			Set<String> result) {
		for (TemplatePart part : template.getParameters(parameterName)) {
			String value = part.getValue().toWiki(true).trim();
			if (StringUtils.isNotBlank(value)) {
				if (NOVALUES.contains(value)) {
					result.add(NOVALUE);
				} else {
					result.add(value);
				}
			}
		}
	}

	private void fillToWikidata(Set<String> source, EntityId property,
			JSONObject result) {
		if (!source.isEmpty()) {
			for (String newValue : source) {
				if (NOVALUE.equals(newValue)) {
					ApiStatement statement = ApiStatement.newStatement(
							property, SnakType.novalue);
					ApiEntity.putProperty(result, property, statement);
				} else {
					ApiStatement statement = ApiStatement
							.newStringValueStatement(property, newValue);
					ApiEntity.putProperty(result, property, statement);
				}
			}
		}
	}

	private void process(Revision revision) throws Exception {

		Map<String, Set<String>> fromPedia = new HashMap<>();

		ArticleFragment fragment = ruWikipediaBot.getXmlParser()
				.parse(revision);
		for (Template template : fragment.getAllTemplates().get(
				TEMPLATE.toLowerCase())) {
			for (Map.Entry<String, EntityId> entry : parametersToMove
					.entrySet()) {
				String parameterName = entry.getKey();
				LinkedHashSet<String> fromPediaSet = new LinkedHashSet<>();
				fromPedia.put(entry.getKey(), fromPediaSet);
				fillFromWikipedia(template, parameterName, fromPediaSet);
				template.removeParameter(parameterName);
			}
		}

		boolean allSetsAreEmptry = fromPedia.values().stream()
				.allMatch(set -> set.isEmpty());
		if (allSetsAreEmptry) {
			return;
		}

		ApiEntity entity = wikidataBot.wgGetEntityBySitelink("ruwiki", revision
				.getPage().getTitle(), EntityProperty.claims);

		Map<String, Set<String>> fromData = new HashMap<>();
		for (Map.Entry<String, EntityId> entry : parametersToMove.entrySet()) {
			EntityId property = entry.getValue();
			LinkedHashSet<String> fromDataSet = new LinkedHashSet<>();
			fromData.put(entry.getKey(), fromDataSet);
			fillFromWikidata(entity, property, fromDataSet);
		}

		final JSONObject newData = new JSONObject();

		for (Map.Entry<String, EntityId> entry : parametersToMove.entrySet()) {
			Set<String> fromPediaSet = fromPedia.get(entry.getKey());
			fromPediaSet.removeAll(fromData.get(entry.getKey()));
			fillToWikidata(fromPediaSet, entry.getValue(), newData);
		}

		if (newData.length() != 0) {
			wikidataBot
					.wgEditEntity(entity, newData,
							"Move [[Q6705326|Automatic taxobox]] parameters from ruwiki to Wikidata");
		}

		ruWikipediaBot.writeContent(revision, fragment.toWiki(false),
				"Move [[Шаблон:Таксон]] parameters to Wikidata", true);
	}

	@Override
	public void run() {
		for (Revision revision : ruWikipediaCache.queryByEmbeddedIn("Шаблон:"
				+ TEMPLATE, Namespace.NSS_MAIN)) {
			try {
				process(revision);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
}
