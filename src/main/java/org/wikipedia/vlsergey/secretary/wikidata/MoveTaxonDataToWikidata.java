package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

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

	private static class PropertyDescriptor {
		final EntityId property;
		final String templateParameter;
		final Function<String, String> toWikidata;

		public PropertyDescriptor(String templateProperty, long propertyid) {
			this.templateParameter = templateProperty;
			this.property = EntityId.property(propertyid);
			this.toWikidata = x -> x;
		}

		public PropertyDescriptor(String templateProperty, long propertyid, Function<String, String> toWikidata) {
			this.templateParameter = templateProperty;
			this.property = EntityId.property(propertyid);
			this.toWikidata = toWikidata;
		}
	}

	private static final String NOVALUE = "(novalue)";

	private static final Set<String> NOVALUES = new HashSet<>(Arrays.asList("notpl", "noipni"));

	private static final List<PropertyDescriptor> parametersToMove = new ArrayList<>();

	private static final String TEMPLATE = "Таксон";

	static {
		parametersToMove.add(new PropertyDescriptor("itis", 815));
		parametersToMove.add(new PropertyDescriptor("ncbi", 685));
		parametersToMove.add(new PropertyDescriptor("eol", 830));
		parametersToMove.add(new PropertyDescriptor("ipni", 961, x -> x.contains("-") ? x : x + "-1"));
		parametersToMove.add(new PropertyDescriptor("tpl", 1070));
	}

	@Autowired
	@Qualifier("ruWikipediaBot")
	private MediaWikiBot ruWikipediaBot;

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	@Autowired
	private WikidataBot wikidataBot;

	private void fillFromWikidata(Entity entity, EntityId property, Set<String> result) {
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

	private void fillFromWikipedia(Template template, PropertyDescriptor descriptor, Set<String> result) {
		for (TemplatePart part : template.getParameters(descriptor.templateParameter)) {
			if (part.getValue() != null) {
				String value = part.getValue().toWiki(true).trim();
				if (StringUtils.isNotBlank(value)) {
					if (NOVALUES.contains(value)) {
						result.add(NOVALUE);
					} else {
						result.add(descriptor.toWikidata.apply(value));
					}
				}
			}
		}
	}

	private void fillToWikidata(Set<String> source, EntityId property, JSONObject result) {
		if (!source.isEmpty()) {
			for (String newValue : source) {
				if (NOVALUE.equals(newValue)) {
					ApiStatement statement = ApiStatement.newStatement(property, SnakType.novalue);
					ApiEntity.putProperty(result, property, statement);
				} else {
					ApiStatement statement = ApiStatement.newStringValueStatement(property, newValue);
					ApiEntity.putProperty(result, property, statement);
				}
			}
		}
	}

	private void process(Revision revision) throws Exception {

		Map<String, Set<String>> fromPedia = new HashMap<>();

		ArticleFragment fragment = ruWikipediaBot.getXmlParser().parse(revision);
		if (!fragment.getAllTemplates().containsKey(TEMPLATE.toLowerCase())) {
			return;
		}

		for (Template template : fragment.getAllTemplates().get(TEMPLATE.toLowerCase())) {
			for (PropertyDescriptor descriptor : parametersToMove) {
				LinkedHashSet<String> fromPediaSet = new LinkedHashSet<>();
				fromPedia.put(descriptor.templateParameter, fromPediaSet);
				fillFromWikipedia(template, descriptor, fromPediaSet);
				template.removeParameter(descriptor.templateParameter);
			}
		}

		boolean allSetsAreEmptry = fromPedia.values().stream().allMatch(set -> set.isEmpty());
		if (allSetsAreEmptry) {
			return;
		}

		ApiEntity entity = wikidataBot.wgGetEntityBySitelink("ruwiki", revision.getPage().getTitle(),
				EntityProperty.claims);

		if (entity == null) {
			return;
		}

		Map<String, Set<String>> fromData = new HashMap<>();
		for (PropertyDescriptor descriptor : parametersToMove) {
			EntityId property = descriptor.property;
			LinkedHashSet<String> fromDataSet = new LinkedHashSet<>();
			fromData.put(descriptor.templateParameter, fromDataSet);
			fillFromWikidata(entity, property, fromDataSet);
		}

		final JSONObject newData = new JSONObject();

		for (PropertyDescriptor descriptor : parametersToMove) {
			Set<String> fromPediaSet = fromPedia.get(descriptor.templateParameter);
			fromPediaSet.removeAll(fromData.get(descriptor.templateParameter));
			fillToWikidata(fromPediaSet, descriptor.property, newData);
		}

		if (newData.length() != 0) {
			wikidataBot.wgEditEntity(entity, newData,
					"Move [[Q6705326|Automatic taxobox]] parameters from ruwiki to Wikidata");
		}

		ruWikipediaBot.writeContent(revision, fragment.toWiki(false), "Move [[Шаблон:Таксон]] parameters to Wikidata",
				true);
	}

	@Override
	public void run() {
		for (Revision revision : ruWikipediaCache.queryByEmbeddedIn("Шаблон:" + TEMPLATE, Namespace.NSS_MAIN)) {
			try {
				process(revision);
			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException(e);
			}
		}
	}
}
