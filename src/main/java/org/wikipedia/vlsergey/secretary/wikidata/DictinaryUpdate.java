package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

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
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.NativeEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

public class DictinaryUpdate implements Runnable {

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

		SortedMap<EntityId, List<String>> map = new TreeMap<>();

		for (Revision revision : wikidataCache.queryByBacklinks(propertyPage.getId(), Namespace.MAIN)) {

			String content = revision.getContent();
			JSONObject jsonObject = new JSONObject(content);
			Entity entity = new NativeEntity(jsonObject);

			List<String> values = new ArrayList<>();
			for (Statement statement : entity.getClaims(property)) {
				if (statement.hasValue()) {
					values.add(statement.getStringValue().getValue());
				}
			}
			if (!values.isEmpty()) {
				map.put(entity.getId(), values);
			}
		}

		StringBuilder builder = new StringBuilder("return {\n");
		for (Map.Entry<EntityId, List<String>> entry : map.entrySet()) {
			builder.append(entry.getKey().toString() + " = {");
			for (String value : entry.getValue()) {
				builder.append("'");
				builder.append(value.replace("'", "\\'"));
				builder.append("', ");
			}
			builder.append("},\n");
		}
		builder.append("};\n");

		ruWikipediaBot.writeContent("Модуль:Wikidata:Dictionary/" + property.toString().toUpperCase(), null,
				builder.toString(), null, "Update dictionary of [[:d:Property:" + property.toString().toUpperCase()
						+ "]]", true, false);
	}

	public void setProperty(EntityId propertyId) {
		this.property = propertyId;
	}
}
