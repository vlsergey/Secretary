package org.wikipedia.vlsergey.secretary.cache.wikidata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.DefaultMapEntry;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.functions.MultiresultFunction;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityProperty;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class SitelinksCache {

	@Autowired
	private StoredSitelinksDao storedSitelinksDao;

	@Autowired
	private WikidataBot wikidataBot;

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	public MultiresultFunction<Revision, Map.Entry<Revision, Entity>> getWithEntitiesF(Project project) {
		return new MultiresultFunction<Revision, Map.Entry<Revision, Entity>>() {
			@Override
			public Iterable<Map.Entry<Revision, Entity>> apply(Iterable<? extends Revision> latestRevisions) {
				final String site = project.getCode();
				final Set<String> titlesToSearch = new LinkedHashSet<>();

				final Set<Long> wikidataRevisionsIdsToCheck = new HashSet<>();
				for (Revision revision : latestRevisions) {
					String pageTitle = revision.getPage().getTitle();
					titlesToSearch.add(pageTitle);
					Long revisionId = storedSitelinksDao.findMaxRevisionByPageTitle(site, pageTitle);
					if (revisionId != null) {
						wikidataRevisionsIdsToCheck.add(revisionId);
					}
				}

				Set<Long> wikidataPageIdsToCheck = new HashSet<>();
				for (Long revisionId : wikidataRevisionsIdsToCheck) {
					Revision revision = wikidataCache.queryRevision(revisionId);
					if (revision != null) {
						wikidataPageIdsToCheck.add(revision.getPage().getId());
					}
				}

				Map<String, Entity> found = new HashMap<>();
				for (Revision wikidataRevision : wikidataCache.queryLatestByPageIds(wikidataPageIdsToCheck)) {
					// update latest
					if (!wikidataRevisionsIdsToCheck.contains(wikidataRevision.getId())) {
						storedSitelinksDao.update(wikidataRevision);
					}
					try {
						Entity entity = new Entity(new JSONObject(wikidataRevision.getContent()));
						if (entity.hasSitelink(site)) {
							String title = entity.getSiteLink(site).getTitle();
							if (titlesToSearch.contains(title)) {
								found.put(title, entity);
							}
						}
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				}

				List<Map.Entry<Revision, Entity>> result = new ArrayList<>(titlesToSearch.size());
				for (Revision revision : latestRevisions) {
					String pageTitle = revision.getPage().getTitle();
					if (found.containsKey(pageTitle)) {
						result.add(new DefaultMapEntry(revision, found.get(pageTitle)));
					} else {
						final Entity apiEntityInfo = wikidataBot.wgGetEntityBySitelink(site, pageTitle,
								EntityProperty.info);
						if (apiEntityInfo != null) {
							Long lastRevisionId = apiEntityInfo.getLastRevisionId();
							Revision wikidataRevision = wikidataCache.queryRevision(lastRevisionId);
							storedSitelinksDao.update(wikidataRevision);
							final Entity apiEntity = new Entity(new JSONObject(wikidataRevision.getContent()));
							result.add(new DefaultMapEntry(revision, apiEntity));
						} else {
							result.add(new DefaultMapEntry(revision, null));
						}
					}
				}

				return result;
			}

		}.makeBatched(2048);
	}
}
