package org.wikipedia.vlsergey.secretary.wikidata;

import java.time.Instant;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class DictinaryFlagsUpdate implements Runnable {

	private static final EntityId ENTITY_city_state = EntityId.item(133442);
	private static final EntityId ENTITY_confederation = EntityId.item(170156);
	private static final EntityId ENTITY_country = EntityId.item(6256);
	private static final EntityId ENTITY_empyre = EntityId.item(48349);
	private static final EntityId ENTITY_former_country = EntityId.item(16905642);
	private static final EntityId ENTITY_sovereign_state = EntityId.item(3624078);
	private static final EntityId ENTITY_state = EntityId.item(7275);
	private static final EntityId ENTITY_uk_member = EntityId.item(3336843);
	private static final EntityId ENTITY_un_member = EntityId.item(160016);
	private static final EntityId ENTITY_usa_unincorporated_territory = EntityId.item(2107324);

	private static final EntityId PROPERTY_BEGIN = EntityId.property(580);
	private static final EntityId PROPERTY_FLAG = EntityId.property(41);

	private static final EntityId[] TOCHECK = { ENTITY_city_state, ENTITY_confederation, ENTITY_country,
			ENTITY_former_country, ENTITY_empyre, ENTITY_sovereign_state, ENTITY_state, ENTITY_uk_member,
			ENTITY_un_member, ENTITY_usa_unincorporated_territory };

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

		SortedMap<EntityId, SortedMap<Long, String>> map = new TreeMap<>();

		for (EntityId toCheck : TOCHECK) {
			final Revision stateTypeItemRev = wikidataCache.queryLatestRevision(toCheck.toString().toUpperCase());
			final Page stateTypeItemPage = stateTypeItemRev.getPage();

			for (Revision revision : wikidataCache.queryByBacklinks(stateTypeItemPage.getId(), Namespace.MAIN)) {
				Entity entity = new ApiEntity(new JSONObject(revision.getContent()));

				SortedMap<Long, String> values = new TreeMap<>();
				for (Statement statement : entity.getClaims(PROPERTY_FLAG)) {
					Long start = Long.MIN_VALUE;
					for (Snak begin : statement.getQualifiers(PROPERTY_BEGIN)) {
						if (begin.hasValue()) {
							try {
								start = Instant.from(begin.getTimeValue().floor()).getEpochSecond() * 1000;
								break;
							} catch (Exception exc) {
								continue;
							}
						}
					}

					if (statement.hasValue()) {
						values.put(start, statement.getStringValue().getValue());
					} else {
						values.put(start, StringUtils.EMPTY);
					}
				}
				if (!values.isEmpty()) {
					map.put(entity.getId(), values);
				}
			}
		}

		StringBuilder builder = new StringBuilder("return {\n");
		for (Map.Entry<EntityId, SortedMap<Long, String>> entry : map.entrySet()) {
			builder.append(entry.getKey().toString() + " = {");

			for (Map.Entry<Long, String> flag : entry.getValue().entrySet()) {
				builder.append("[");
				builder.append(flag.getKey());
				builder.append("]=");
				if (StringUtils.isBlank(flag.getValue())) {
					builder.append("false");
				} else {
					builder.append("'");
					builder.append(flag.getValue().replace("'", "\\'"));
					builder.append("'");
				}
				builder.append(", ");
			}
			builder.append("},\n");
		}
		builder.append("};\n");

		ruWikipediaBot.writeContent("Модуль:Wikidata:Dictionary/Flags", null, builder.toString(), null,
				"Update of flags dictionary", true, false);
	}
}
