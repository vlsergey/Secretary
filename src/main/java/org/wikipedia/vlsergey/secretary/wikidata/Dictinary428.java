package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.NativeEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class Dictinary428 implements Runnable {

	private static final EntityId PROPERTY_BAA = EntityId.property(428);

	@Autowired
	@Qualifier("ruWikipediaBot")
	private MediaWikiBot ruWikipediaBot;

	@Autowired
	private WikidataBot wikidataBot;

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	@Override
	public void run() {

		SortedMap<EntityId, List<String>> map = new TreeMap<>();

		for (Revision revision : wikidataCache.queryByBacklinks(12290021l,
				Namespace.MAIN)) {

			String content = revision.getContent();
			JSONObject jsonObject = new JSONObject(content);
			Entity entity = new NativeEntity(jsonObject);

			List<String> values = new ArrayList<>();
			for (Statement statement : entity.getClaims(PROPERTY_BAA)) {
				if (statement.hasValue()) {
					values.add(statement.getStringValue().getValue());
				}
			}
			if (!values.isEmpty()) {
				map.put(entity.getId(), values);
			}
		}

		System.out.println(map);
	}
}
