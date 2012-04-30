package org.wikipedia.vlsergey.secretary.cache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.functions.MultiresultFunction;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.JwbfException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

@Transactional(propagation = Propagation.NEVER)
public class WikiCache {

	private static final Logger logger = LoggerFactory.getLogger(WikiCache.class);

	
	private MediaWikiBot mediaWikiBot;

	
	private StoredRevisionDao storedRevisionDao;

	public List<Revision> queryLatestContentByPageIds(Iterable<Long> pageIds) throws ActionException, ProcessException {
		logger.info("queryLatestContentByPageIds: " + pageIds);

		Map<Long, Long> pageIdToLatestRevision = new LinkedHashMap<Long, Long>();
		for (Revision revision : mediaWikiBot.queryRevisionsByPageIds(pageIds, RevisionPropery.IDS,
				RevisionPropery.TIMESTAMP)) {
			// update info in DB
			revision = storedRevisionDao.getOrCreate(revision);

			pageIdToLatestRevision.put(revision.getPage().getId(), revision.getId());
		}

		Map<Long, Revision> resultMap = new LinkedHashMap<Long, Revision>(pageIdToLatestRevision.size());
		List<Long> toLoad = new ArrayList<Long>(pageIdToLatestRevision.size());

		for (Long pageId : pageIds) {
			Long latestRevisionId = pageIdToLatestRevision.get(pageId);
			if (latestRevisionId == null) {
				logger.warn("Page #" + pageId + " has no revisions");
				continue;
			}

			Revision stored = storedRevisionDao.getRevisionById(latestRevisionId);
			if (stored != null && StringUtils.isNotEmpty(stored.getContent())) {
				resultMap.put(latestRevisionId, stored);
			} else {
				toLoad.add(latestRevisionId);
			}
		}

		for (Revision revision : mediaWikiBot.queryRevisionsByRevisionIds(toLoad, RevisionPropery.IDS,
				RevisionPropery.TIMESTAMP, RevisionPropery.CONTENT)) {
			// update cache
			revision = storedRevisionDao.getOrCreate(revision);
			resultMap.put(revision.getId(), revision);
		}

		List<Revision> result = new ArrayList<Revision>();
		for (Long pageId : pageIds) {
			Long latestRevisionId = pageIdToLatestRevision.get(pageId);
			if (latestRevisionId == null)
				continue;

			Revision revision = resultMap.get(latestRevisionId);
			if (revision == null) {
				logger.warn("Page #" + pageId + " has no revisions");
				continue;
			}

			result.add(revision);
		}
		return result;
	}

	public MultiresultFunction<Long, Revision> queryLatestContentByPageIdsF() {
		return new MultiresultFunction<Long, Revision>() {
			@Override
			public Iterable<Revision> apply(Iterable<Long> pageIds) {
				return queryLatestContentByPageIds(pageIds);
			}
		};
	}

	@Transactional(propagation = Propagation.NEVER)
	public Revision queryLatestRevision(Long pageId) {
		logger.debug("queryLatestRevision(" + pageId + ")");

		Revision latest = mediaWikiBot.queryRevisionByPageId(pageId, new RevisionPropery[] { RevisionPropery.IDS,
				RevisionPropery.TIMESTAMP });

		if (latest == null)
			return null;

		Revision stored = storedRevisionDao.getOrCreate(latest);
		if (stored != null && StringUtils.isNotEmpty(stored.getContent()))
			return stored;

		Revision withContent = mediaWikiBot.queryRevisionByRevisionId(latest.getId(), new RevisionPropery[] {
				RevisionPropery.IDS, RevisionPropery.CONTENT, RevisionPropery.TIMESTAMP }, false);

		if (withContent == null)
			// deleted
			return null;

		storedRevisionDao.getOrCreate(withContent);
		return withContent;
	}

	public Revision queryLatestRevision(String pageTitle) {
		logger.debug("queryLatestRevision('" + pageTitle + "')");

		Revision latest = mediaWikiBot.queryRevisionLatest(pageTitle, new RevisionPropery[] { RevisionPropery.IDS,
				RevisionPropery.TIMESTAMP });

		if (latest == null)
			return null;

		Revision stored = storedRevisionDao.getOrCreate(latest);
		if (StringUtils.isNotEmpty(stored.getContent()))
			return stored;

		latest = mediaWikiBot.queryRevisionByRevisionId(latest.getId(), new RevisionPropery[] { RevisionPropery.IDS,
				RevisionPropery.CONTENT, RevisionPropery.TIMESTAMP }, false);
		return latest;
	}

	@Transactional(propagation = Propagation.NEVER)
	public String queryLatestRevisionContent(Long pageId) throws JwbfException {
		logger.debug("queryLatestRevisionContent(" + pageId + ")");

		Revision withContent = queryLatestRevision(pageId);
		if (withContent == null)
			return null;

		return withContent.getContent();
	}

	@Transactional(propagation = Propagation.NEVER)
	public String queryLatestRevisionContent(String pageTitle) throws JwbfException {
		logger.debug("queryLatestRevisionContent('" + pageTitle + "')");

		Revision latest = queryLatestRevision(pageTitle);

		if (latest == null)
			// deleted or not found
			return null;

		Revision stored = storedRevisionDao.getOrCreate(latest);
		return stored.getContent();
	}

	@Transactional(propagation = Propagation.NEVER)
	public String queryRevisionContent(Long revisionId) throws JwbfException {
		logger.debug("queryLatestRevisionContent(" + revisionId + ")");

		Revision stored = storedRevisionDao.getRevisionById(revisionId);
		if (stored != null && StringUtils.isNotEmpty(stored.getContent()))
			return stored.getContent();

		Revision withContent = mediaWikiBot.queryRevisionByRevisionId(revisionId, new RevisionPropery[] {
				RevisionPropery.IDS, RevisionPropery.CONTENT, RevisionPropery.TIMESTAMP }, false);

		if (withContent == null)
			// deleted
			return null;

		storedRevisionDao.getOrCreate(withContent);
		return withContent.getContent();
	}
}
