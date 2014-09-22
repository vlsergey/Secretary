package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityProperty;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

class EntityByLinkResolver implements Function<String, Entity> {

	private final Map<String, Long> cache = new HashMap<>();

	private final TitleResolver titleResolver;

	private final WikidataBot wikidataBot;

	private final WikiCache wikidataCache;

	EntityByLinkResolver(WikiCache wikidataCache, final TitleResolver titleResolver) {
		this.wikidataCache = wikidataCache;
		this.wikidataBot = (WikidataBot) wikidataCache.getMediaWikiBot();
		this.titleResolver = titleResolver;
	}

	@Override
	public ApiEntity apply(String wikiPageTitle) {
		synchronized (this) {
			if (cache.containsKey(wikiPageTitle)) {
				Long revisionId = cache.get(wikiPageTitle);
				if (revisionId == null) {
					return null;
				}
				return new ApiEntity(new JSONObject(wikidataCache.queryRevisionContent(revisionId)));
			}

			Revision value = build(wikiPageTitle);
			if (value == null) {
				cache.put(wikiPageTitle, null);
				return null;
			}

			cache.put(wikiPageTitle, value.getId());
			final ApiEntity result = new ApiEntity(new JSONObject(value.getContent()));
			titleResolver.update(result);
			return result;
		}
	}

	private Revision build(final String wikiPageTitle) {
		try {
			ApiEntity apiEntity = wikidataBot.wgGetEntityBySitelink("ruwiki", wikiPageTitle, EntityProperty.info);
			if (apiEntity == null) {
				return null;
			}

			return wikidataCache.queryRevision(apiEntity.getLastRevisionId());
		} catch (Exception exc) {
			// unable to resolve
			return null;
		}
	}
}