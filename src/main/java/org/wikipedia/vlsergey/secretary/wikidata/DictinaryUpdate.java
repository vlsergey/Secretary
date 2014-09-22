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

import org.apache.commons.lang.StringUtils;
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
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ValueType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

public class DictinaryUpdate implements Runnable {

	private static final Log log = LogFactory.getLog(DictinaryUpdate.class);

	@Autowired
	private List<DictionaryUpdateListener> dictionaryUpdateListeners;

	private EntityId property;

	@Autowired
	@Qualifier("ruWikipediaBot")
	private MediaWikiBot ruWikipediaBot;

	@Autowired
	private WikidataBot wikidataBot;

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	public EntityId getProperty() {
		return property;
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
				Entity entity = new ApiEntity(jsonObject);

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
			for (Revision revision : wikidataCache.queryLatestContentByPageTitles(toCheckPageTitles, false)) {
				String content = revision.getContent();
				JSONObject jsonObject = new JSONObject(content);
				Entity entity = new ApiEntity(jsonObject);
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

		StringBuilder builder = new StringBuilder("return {\n");
		for (Map.Entry<EntityId, List<Statement>> entry : map.entrySet()) {
			builder.append(entry.getKey().toString() + " = {");
			for (Statement value : entry.getValue()) {
				if (value.hasValue() && value.getValueType() == ValueType.STRING) {
					builder.append("'");
					builder.append(value.getStringValue().getValue().replace("'", "\\'"));
					builder.append("', ");
				}
				if (value.hasValue() && value.getValueType() == ValueType.WIKIBASE_ENTITYID) {
					builder.append("'");
					builder.append(localName.apply(value.getMainSnak().getWikibaseEntityIdValue().getEntityId())
							.replace("'", "\\'"));
					builder.append("', ");
				}
			}
			builder.append("},\n");
		}
		builder.append("};\n");

		ruWikipediaBot.writeContent("Модуль:Wikidata:Dictionary/" + property.toString().toUpperCase(), null,
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
}
