package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.LinkedHashSet;
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
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class MoveTaxonDataToWikidata implements Runnable {

	private static final EntityId PROPERTY_ITIS = EntityId.property(815);

	private static final EntityId PROPERTY_NCBI = EntityId.property(685);

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
			if (statement.hasValue()) {
				result.add(statement.getStringValue().getValue());
			}
		}
	}

	private void fillFromWikipedia(Template template, String parameterName,
			Set<String> result) {
		for (TemplatePart part : template.getParameters(parameterName)) {
			String value = part.getValue().toWiki(true).trim();
			if (StringUtils.isNotBlank(value)) {
				result.add(value);
			}
		}
	}

	private void fillToWikidata(Set<String> source, EntityId property,
			JSONObject result) {
		if (!source.isEmpty()) {
			for (String newValue : source) {
				ApiStatement statement = ApiStatement.newStringValueStatement(
						property, newValue);
				ApiEntity.putProperty(result, property, statement);
			}
		}
	}

	private void process(Revision revision) throws Exception {

		Set<String> itisFromPedia = new LinkedHashSet<>();
		Set<String> ncbiFromPedia = new LinkedHashSet<>();

		ArticleFragment fragment = ruWikipediaBot.getXmlParser()
				.parse(revision);
		for (Template template : fragment.getAllTemplates().get("taxobox")) {
			fillFromWikipedia(template, "itis", itisFromPedia);
			fillFromWikipedia(template, "ncbi", ncbiFromPedia);
			template.removeParameter("itis");
			template.removeParameter("ncbi");
		}

		if (itisFromPedia.isEmpty() && ncbiFromPedia.isEmpty()) {
			return;
		}

		ApiEntity entity = wikidataBot.wgGetEntityBySitelink("ruwiki", revision
				.getPage().getTitle(), EntityProperty.claims);

		Set<String> itisFromData = new LinkedHashSet<>();
		Set<String> ncbiFromData = new LinkedHashSet<>();
		fillFromWikidata(entity, PROPERTY_ITIS, itisFromData);
		fillFromWikidata(entity, PROPERTY_NCBI, ncbiFromData);

		final JSONObject newData = new JSONObject();

		itisFromPedia.removeAll(itisFromData);
		ncbiFromPedia.removeAll(ncbiFromData);

		fillToWikidata(itisFromPedia, PROPERTY_ITIS, newData);
		fillToWikidata(ncbiFromPedia, PROPERTY_NCBI, newData);

		if (newData.length() != 0) {
			wikidataBot
					.wgEditEntity(entity, newData,
							"Move [[Q52496|Taxobox]] parameters from ruwiki to Wikidata");
		}

		ruWikipediaBot.writeContent(revision, fragment.toWiki(false),
				"Move Taxobox parameters to Wikidata", true);
	}

	@Override
	public void run() {
		for (Revision revision : ruWikipediaCache.queryByEmbeddedIn(
				"Шаблон:Taxobox", Namespace.NSS_MAIN)) {
			try {
				process(revision);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
