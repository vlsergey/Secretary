package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.NativeEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class UpdateConstraintViolations implements Runnable {

	public static final EntityId PROPERTY_VIAF = EntityId.property(214);

	@Autowired
	private WikidataBot wikidataBot;

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	@Override
	public void run() {

		Map<String, List<Entity>> uniqueViolations = new TreeMap<>();
		Map<Entity, List<String>> singleValueViolations = new TreeMap<>();

		for (Revision revision : wikidataCache.queryByLinks(Long.valueOf(13576476l), Namespace.MAIN)) {
			String content = revision.getContent();

			JSONObject jsonObject = new JSONObject(content);
			Entity entity = new NativeEntity(jsonObject);

			for (Statement statement : entity.getClaims(PROPERTY_VIAF)) {
				final Snak mainSnak = statement.getMainSnak();
				if (mainSnak.getSnakType() == SnakType.value) {
					String value = mainSnak.getStringValue().getValue();
					if (!uniqueViolations.containsKey(value)) {
						uniqueViolations.put(value, new ArrayList<Entity>(1));
					}
					uniqueViolations.get(value).add(entity);

					if (!singleValueViolations.containsKey(entity)) {
						singleValueViolations.put(entity, new ArrayList<String>(1));
					}
					singleValueViolations.get(entity).add(value);
				}
			}
		}

		{
			System.out.println("«Unique value» violations");

			int count = 0;
			for (Map.Entry<String, List<Entity>> entry : uniqueViolations.entrySet()) {
				final List<Entity> entities = entry.getValue();
				if (entities.size() == 1) {
					continue;
				}
				count++;

				Collections.sort(entities, (o1, o2) -> Long.compare(o1.getId().getId(), o2.getId().getId()));

				StringBuilder builder = new StringBuilder("* [//viaf.org/viaf/" + entry.getKey() + "/ "
						+ entry.getKey() + "]: ");
				for (Entity entity : entities) {
					builder.append("{{Q|");
					builder.append(entity.getId().getId());
					builder.append("}}, ");
				}
				builder.setLength(builder.length() - 2);
				System.out.println(builder);
			}
			System.out.println(count);
		}

		{
			System.out.println("«Single value» violations");

			int count = 0;
			for (Map.Entry<Entity, List<String>> entry : singleValueViolations.entrySet()) {
				final List<String> values = entry.getValue();
				if (values.size() == 1) {
					continue;
				}
				count++;

				StringBuilder builder = new StringBuilder("* [[" + entry.getKey().getId().toString() + "]]: ");
				for (String value : values) {
					builder.append("[//viaf.org/viaf/" + value + "/ " + value + "], ");
				}
				builder.setLength(builder.length() - 2);
				System.out.println(builder);
			}
			System.out.println(count);
		}

	}
}
