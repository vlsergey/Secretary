package org.wikipedia.vlsergey.secretary.cache;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.functions.MultiresultFunction;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.JwbfException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

@Transactional(propagation = Propagation.NEVER)
public class WikiCache {

	public static final RevisionPropery[] CACHED = { RevisionPropery.IDS, RevisionPropery.TIMESTAMP,
			RevisionPropery.CONTENT, RevisionPropery.USER, RevisionPropery.SIZE };

	public static final RevisionPropery[] FAST = { RevisionPropery.IDS, RevisionPropery.TIMESTAMP,
			RevisionPropery.USER, RevisionPropery.SIZE };

	private static final Logger log = LoggerFactory.getLogger(WikiCache.class);

	private static boolean isCacheRecordValid(Revision stored) {
		return stored != null && stored.hasContent() && StringUtils.isNotEmpty(stored.getXml())
				&& StringUtils.isNotEmpty(stored.getUser()) && stored.getTimestamp() != null
				&& stored.getTimestamp().getTime() != 0 && stored.getSize() != null && stored.getSize().longValue() > 0;
	}

	private MediaWikiBot mediaWikiBot;

	private Project project;

	@Autowired
	private StoredRevisionDao storedRevisionDao;

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public Project getProject() {
		return project;
	}

	public Iterable<Revision> queryAllRevisions(Long pageId, Direction direction) {
		List<Revision> revisionIdHolders = new ArrayList<Revision>(getMediaWikiBot().queryRevisionsByPageId(pageId,
				null, direction, FAST));

		return queryRevisionsImplF().apply(revisionIdHolders);
	}

	public Iterable<Revision> queryAllRevisions(Page page, Direction direction) {
		List<Revision> revisionIdHolders = new ArrayList<Revision>(getMediaWikiBot().queryRevisionsByPageId(
				page.getId(), null, direction, FAST));

		return queryRevisionsImplF().apply(revisionIdHolders);
	}

	public Iterable<Revision> queryAllRevisions(String pageTitle, Direction direction) {
		List<Revision> revisionIdHolders = new ArrayList<Revision>(getMediaWikiBot().queryRevisionsByPageTitle(
				pageTitle, null, direction, FAST));

		return queryRevisionsImplF().apply(revisionIdHolders);
	}

	public Iterable<Revision> queryContentByPagesAndRevisions(Iterable<ParsedPage> pagesWithLatestsRevisions)
			throws ActionException, ProcessException {
		return queryContentByPagesAndRevisionsF().makeBatched(2048).apply(pagesWithLatestsRevisions);
	}

	public MultiresultFunction<ParsedPage, Revision> queryContentByPagesAndRevisionsF() throws ActionException,
			ProcessException {

		return new MultiresultFunction<ParsedPage, Revision>() {

			@Override
			public Iterable<Revision> apply(Iterable<ParsedPage> pagesWithLatestsRevisions) {

				Map<Long, Long> pageIdToLatestRevision = new LinkedHashMap<Long, Long>();
				for (ParsedPage page : pagesWithLatestsRevisions) {
					if (page.getRevisions() == null || page.getRevisions().isEmpty()) {
						continue;
					}

					Revision revision = page.getRevisions().get(0);
					pageIdToLatestRevision.put(page.getId(), revision.getId());
				}

				Map<Long, Revision> resultMap = new LinkedHashMap<Long, Revision>(pageIdToLatestRevision.size());
				List<Long> toLoad = new ArrayList<Long>(pageIdToLatestRevision.size());

				for (Long pageId : pageIdToLatestRevision.keySet()) {
					Long latestRevisionId = pageIdToLatestRevision.get(pageId);

					Revision stored = storedRevisionDao.getRevisionById(getProject(), latestRevisionId);
					if (isCacheRecordValid(stored)) {
						resultMap.put(latestRevisionId, stored);
					} else {
						toLoad.add(latestRevisionId);
					}
				}

				for (Revision revision : mediaWikiBot.queryRevisionsByRevisionIdsF(true, CACHED).apply(toLoad)) {
					// update cache
					revision = storedRevisionDao.getOrCreate(getProject(), revision);
					resultMap.put(revision.getId(), revision);
				}

				List<Revision> result = new ArrayList<Revision>();
				for (Long pageId : pageIdToLatestRevision.keySet()) {
					Long latestRevisionId = pageIdToLatestRevision.get(pageId);
					if (latestRevisionId == null)
						continue;

					Revision revision = resultMap.get(latestRevisionId);
					if (revision == null) {
						log.warn("Page #" + pageId + " has no revisions");
						continue;
					}

					result.add(revision);
				}
				return result;
			}
		};
	}

	public List<Revision> queryLatestContentByPageIds(Iterable<Long> pageIds) throws ActionException, ProcessException {
		log.info("queryLatestContentByPageIds: " + pageIds);

		Map<Long, Long> pageIdToLatestRevision = new LinkedHashMap<Long, Long>();
		for (Revision revision : mediaWikiBot.queryLatestRevisionsByPageIds(pageIds, FAST)) {
			// update info in DB
			revision = storedRevisionDao.getOrCreate(getProject(), revision);

			pageIdToLatestRevision.put(revision.getPage().getId(), revision.getId());
		}

		Map<Long, Revision> resultMap = new LinkedHashMap<Long, Revision>(pageIdToLatestRevision.size());
		List<Long> toLoad = new ArrayList<Long>(pageIdToLatestRevision.size());

		for (Long pageId : pageIds) {
			Long latestRevisionId = pageIdToLatestRevision.get(pageId);
			if (latestRevisionId == null) {
				log.warn("Page #" + pageId + " has no revisions");
				continue;
			}

			Revision stored = storedRevisionDao.getRevisionById(getProject(), latestRevisionId);
			if (isCacheRecordValid(stored)) {
				resultMap.put(latestRevisionId, stored);
			} else {
				toLoad.add(latestRevisionId);
			}
		}

		for (Revision revision : mediaWikiBot.queryRevisionsByRevisionIdsF(true, CACHED).apply(toLoad)) {
			// update cache
			revision = storedRevisionDao.getOrCreate(getProject(), revision);
			resultMap.put(revision.getId(), revision);
		}

		List<Revision> result = new ArrayList<Revision>();
		for (Long pageId : pageIds) {
			Long latestRevisionId = pageIdToLatestRevision.get(pageId);
			if (latestRevisionId == null)
				continue;

			Revision revision = resultMap.get(latestRevisionId);
			if (revision == null) {
				log.warn("Page #" + pageId + " has no revisions");
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
		log.debug("queryLatestRevision(" + pageId + ")");

		Revision latest = mediaWikiBot.queryLatestRevision(pageId, FAST);

		if (latest == null)
			return null;

		Revision stored = storedRevisionDao.getOrCreate(getProject(), latest);
		if (isCacheRecordValid(stored))
			return stored;

		Revision withContent = mediaWikiBot.queryRevisionByRevisionId(latest.getId(), true, CACHED);

		if (withContent == null)
			// deleted
			return null;

		storedRevisionDao.getOrCreate(getProject(), withContent);
		return withContent;
	}

	public Revision queryLatestRevision(String pageTitle) {
		log.debug("queryLatestRevision('" + pageTitle + "')");

		Revision latest = mediaWikiBot.queryLatestRevision(pageTitle, false, FAST);

		if (latest == null)
			return null;

		Revision stored = storedRevisionDao.getOrCreate(getProject(), latest);
		if (isCacheRecordValid(stored))
			return stored;

		latest = mediaWikiBot.queryRevisionByRevisionId(latest.getId(), true, CACHED);
		return latest;
	}

	@Transactional(propagation = Propagation.NEVER)
	public String queryLatestRevisionContent(Long pageId) throws JwbfException {
		log.debug("queryLatestRevisionContent(" + pageId + ")");

		Revision withContent = queryLatestRevision(pageId);
		if (withContent == null)
			return null;

		return withContent.getContent();
	}

	@Transactional(propagation = Propagation.NEVER)
	public String queryLatestRevisionContent(String pageTitle) throws JwbfException {
		log.debug("queryLatestRevisionContent('" + pageTitle + "')");

		Revision latest = queryLatestRevision(pageTitle);

		if (latest == null)
			// deleted or not found
			return null;

		Revision stored = storedRevisionDao.getOrCreate(getProject(), latest);
		return stored.getContent();
	}

	@Transactional(propagation = Propagation.NEVER)
	public StoredRevision queryRevision(Long revisionId) throws JwbfException {
		log.debug("queryRevision(" + revisionId + ")");

		StoredRevision stored = storedRevisionDao.getRevisionById(getProject(), revisionId);
		if (isCacheRecordValid(stored)) {
			return stored;
		}

		Revision withContent = mediaWikiBot.queryRevisionByRevisionId(revisionId, false, CACHED);

		if (withContent == null)
			// deleted
			return null;

		StoredRevision revision = storedRevisionDao.getOrCreate(getProject(), withContent);
		return revision;
	}

	@Transactional(propagation = Propagation.NEVER)
	public StoredRevision queryRevision(Revision revision) {
		if (revision == null) {
			throw new NullArgumentException("revision");
		}
		if (revision instanceof StoredRevision) {
			return ((StoredRevision) revision);
		}
		return queryRevision(revision.getId());
	}

	@Transactional(propagation = Propagation.NEVER)
	public String queryRevisionContent(Long revisionId) throws JwbfException {
		log.debug("queryRevision(" + revisionId + ")");

		Revision stored = storedRevisionDao.getRevisionById(getProject(), revisionId);
		if (stored != null && StringUtils.isNotEmpty(stored.getContent()))
			return stored.getContent();

		Revision withContent = mediaWikiBot.queryRevisionByRevisionId(revisionId, false, CACHED);

		if (withContent == null)
			// deleted
			return null;

		storedRevisionDao.getOrCreate(getProject(), withContent);
		return withContent.getContent();
	}

	public Iterable<Revision> queryRevisions(Iterable<Revision> revisions) {
		return queryRevisionsImplF().apply(revisions);
	}

	public Iterable<Revision> queryRevisions(Page page, Long startRevId, Direction direction) {
		List<Revision> revisionIdHolders = new ArrayList<Revision>(getMediaWikiBot().queryRevisionsByPageId(
				page.getId(), startRevId, direction, FAST));

		return queryRevisionsImplF().apply(revisionIdHolders);
	}

	private MultiresultFunction<Revision, Revision> queryRevisionsImplF() {
		return new MultiresultFunction<Revision, Revision>() {
			@Override
			public Iterable<Revision> apply(Iterable<Revision> revisionIdHolders) {
				List<Long> toLoad = new ArrayList<Long>();
				Map<Long, Revision> resultMap = new LinkedHashMap<Long, Revision>();

				for (Revision storedRevision : storedRevisionDao.getOrCreate(getProject(), revisionIdHolders)) {
					final Long revisionId = storedRevision.getId();
					if (isCacheRecordValid(storedRevision)) {
						resultMap.put(revisionId, storedRevision);
					} else {
						toLoad.add(revisionId);
					}
				}

				Iterable<Revision> revisionsWithContent = mediaWikiBot.queryRevisionsByRevisionIdsF(true, CACHED)
						.apply(toLoad);
				// update cache
				for (Revision revision : storedRevisionDao.getOrCreate(getProject(), revisionsWithContent)) {
					resultMap.put(revision.getId(), revision);
				}

				List<Revision> result = new ArrayList<Revision>();
				for (Revision revisionIdHolder : revisionIdHolders) {
					Revision revision = resultMap.get(revisionIdHolder.getId());
					if (revision == null)
						continue;
					result.add(revision);
				}

				return result;
			}
		}.makeBatched(100);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public int removePageRevisionsExcept(Page page, Set<Long> preserveRevisionIds) {
		log.debug("Cleanup cache entites for " + page);
		int result = storedRevisionDao.removePageRevisionsExcept(getProject(), page.getId(), preserveRevisionIds);
		log.info("Cleaned " + result + " cache entries of " + page);
		return result;
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setProject(Project project) {
		this.project = project;
	}
}
