package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ValueType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

public class DictinaryUpdate implements Runnable {

	private static final Log log = LogFactory.getLog(DictinaryUpdate.class);

	// @Autowired
	private List<DictionaryUpdateListener> dictionaryUpdateListeners;

	private EntityId property;

	private MediaWikiBot targetProjectBot;

	private Map<String, String> valuesPrefixes;

	@Autowired
	private WikidataBot wikidataBot;

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	public EntityId getProperty() {
		return property;
	}

	public MediaWikiBot getTargetProjectBot() {
		return targetProjectBot;
	}

	public Map<String, String> getValuesPrefixes() {
		return valuesPrefixes;
	}

	@Override
	public void run() {

		final Revision propertyRev = wikidataCache.queryLatestRevision("Property:" + property.toString().toUpperCase());
		final Page propertyPage = propertyRev.getPage();

		SortedMap<EntityId, List<Statement>> map = new TreeMap<>();

		for (Revision revision : wikidataCache.queryByBacklinks(propertyPage.getId(), Namespace.MAIN)) {
			try {
				String content = revision.getContent();
				JSONObject jsonObject = new JSONObject(content);
				Entity entity = new Entity(jsonObject);

				List<Statement> values = new ArrayList<>();
				for (Statement statement : entity.getClaims(property)) {
					if (statement.hasValue()) {
						values.add(statement);
					}
				}
				if (!values.isEmpty()) {
					map.put(entity.getId(), values);
				}
			} catch (RuntimeException exc) {
				log.error("Unable to update dictionary because of error with " + revision + ": " + exc, exc);
				throw exc;
			}
		}

		final Function<EntityId, String> localName;
		{
			TreeSet<EntityId> toCheck = new TreeSet<>();
			for (Map.Entry<EntityId, List<Statement>> entry : map.entrySet()) {
				for (Statement value : entry.getValue()) {
					if (value.hasValue() && value.getValueType() == ValueType.WIKIBASE_ENTITYID) {
						toCheck.add(value.getMainSnak().getWikibaseEntityIdValue().getEntityId());
					}
				}
			}
			final Map<EntityId, String> localNames = new HashMap<>();
			final List<String> toCheckPageTitles = toCheck.stream().map(x -> x.toString()).collect(Collectors.toList());
			for (Revision revision : wikidataCache.queryLatestByPageTitles(toCheckPageTitles, false)) {
				String content = revision.getContent();
				JSONObject jsonObject = new JSONObject(content);
				Entity entity = new Entity(jsonObject);
				if (entity.hasSitelink("ruwiki")) {
					localNames.put(entity.getId(), entity.getSiteLink("ruwiki").getTitle());
				}
			}
			localName = x -> localNames.get(x);
		}

		for (Iterator<Entry<EntityId, List<Statement>>> iterator = map.entrySet().iterator(); iterator.hasNext();) {
			Map.Entry<EntityId, List<Statement>> entry = iterator.next();

			for (Iterator<Statement> iterator2 = entry.getValue().iterator(); iterator2.hasNext();) {
				Statement value = iterator2.next();
				if (value.hasValue() && value.getValueType() == ValueType.WIKIBASE_ENTITYID) {
					if (StringUtils.isEmpty(localName.apply(value.getMainSnak().getWikibaseEntityIdValue()
							.getEntityId()))) {
						iterator2.remove();
						continue;
					}
				}
			}

			if (entry.getValue().isEmpty()) {
				iterator.remove();
				continue;
			}
		}

		StringBuilder builder = new StringBuilder();
		if (this.valuesPrefixes != null && !this.valuesPrefixes.isEmpty()) {
			for (Map.Entry<String, String> valuePrefix : this.valuesPrefixes.entrySet()) {
				builder.append("local " + valuePrefix.getKey() + "='" + valuePrefix.getValue().replace("'", "\\'")
						+ "'\n");
			}
		}

		builder.append("return {\n");
		for (Map.Entry<EntityId, List<Statement>> entry : map.entrySet()) {
			builder.append(entry.getKey().toString() + "={");

			String toAppend = entry
					.getValue()
					.stream()
					.filter(value -> value.hasValue())
					.map(value -> {
						if (value.getValueType() == ValueType.STRING) {
							return value.getStringValue().getValue();
						}
						if (value.hasValue() && value.getValueType() == ValueType.WIKIBASE_ENTITYID) {
							return localName.apply(value.getMainSnak().getWikibaseEntityIdValue().getEntityId());
						}
						return null;
					})
					.filter(x -> x != null)
					.map(x -> {
						// replace with prefix append, if possible
						if (this.valuesPrefixes != null && !this.valuesPrefixes.isEmpty()) {
							for (Map.Entry<String, String> valuePrefix : this.valuesPrefixes.entrySet()) {
								if (x.startsWith(valuePrefix.getValue())) {
									return valuePrefix.getKey() + "..'"
											+ x.substring(valuePrefix.getValue().length()).replace("'", "\\'") + "'";
								}
							}
						}
						return "'" + x.replace("'", "\\'") + "'";
					}).collect(Collectors.joining(","));

			builder.append(toAppend);
			builder.append("},\n");
		}
		builder.append("};\n");

		targetProjectBot.writeContent("Модуль:Wikidata:Dictionary/" + property.toString().toUpperCase(), null,
				builder.toString(), null, "Update dictionary of [[:d:Property:" + property.toString().toUpperCase()
						+ "]]", true, false);

		if (dictionaryUpdateListeners != null) {
			for (DictionaryUpdateListener dictionaryUpdateListener : dictionaryUpdateListeners) {
				try {
					dictionaryUpdateListener.onUpdate(getProperty(), map);
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		}
	}

	public void setProperty(EntityId propertyId) {
		this.property = propertyId;
	}

	public void setTargetProjectBot(MediaWikiBot wikipediaBot) {
		this.targetProjectBot = wikipediaBot;
	}

	public void setValuesPrefixes(Map<String, String> valuesPrefixes) {
		this.valuesPrefixes = valuesPrefixes;
	}
}
