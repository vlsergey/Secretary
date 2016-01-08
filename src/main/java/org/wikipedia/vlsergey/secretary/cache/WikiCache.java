package org.wikipedia.vlsergey.secretary.cache;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.cache.users.StoredUserDao;
import org.wikipedia.vlsergey.secretary.functions.MultiresultFunction;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByCategoryMembers.CmType;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.FilterRedirects;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedRevision;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedUser;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.JwbfException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

@Transactional(propagation = Propagation.NEVER)
public class WikiCache {

	public static final RevisionPropery[] CACHED = { RevisionPropery.IDS, RevisionPropery.TIMESTAMP,
			RevisionPropery.COMMENT, RevisionPropery.CONTENT, RevisionPropery.USER, RevisionPropery.USERID,
			RevisionPropery.SIZE };

	public static final RevisionPropery[] IDS = { RevisionPropery.IDS };

	private static final Logger log = LoggerFactory.getLogger(WikiCache.class);

	private MediaWikiBot mediaWikiBot;

	@Autowired
	private StoredRevisionDaoLock storedRevisionDao;

	@Autowired
	private StoredUserDao storedUserDao;

	public void clear() {
		int cleared = storedRevisionDao.clear(getProject());
		log.info("Cleared " + getProject() + " cache: " + cleared);
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public Project getProject() {
		return getMediaWikiBot().getProject();
	}

	private boolean isCacheRecordValid(Revision stored) {
		if (stored == null) {
			return false;
		}

		return getMediaWikiBot().isCachedRevisionValid(stored);
	}

	public Iterable<Revision> queryByAllPages(Namespace namespace) {
		return queryByPagesAndRevisions(mediaWikiBot.queryPagesWithRevisionByAllPages(namespace, IDS));
	}

	public Iterable<Revision> queryByBacklinks(Long pageId, FilterRedirects filterRedirects, Namespace... namespaces) {
		return queryByPagesAndRevisions(mediaWikiBot.queryPagesWithRevisionByBacklinks(pageId, namespaces,
				filterRedirects, IDS));
	}

	public Iterable<Revision> queryByBacklinks(Long pageId, Namespace... namespaces) {
		return this.queryByBacklinks(pageId, FilterRedirects.NONREDIRECTS, namespaces);
	}

	public Iterable<Revision> queryByCaterogyMembers(String title, Namespace[] namespaces, CmType type) {
		return queryByPagesAndRevisions(mediaWikiBot.queryPagesWithRevisionByCategoryMembers(title, namespaces, type,
				IDS));
	}

	public Iterable<Revision> queryByEmbeddedIn(String title, Namespace... namespaces) {
		return queryByPagesAndRevisions(mediaWikiBot.queryPagesWithRevisionByEmbeddedIn(title, namespaces, IDS));
	}

	public Iterable<Revision> queryByLinks(Long pageId, Namespace... namespaces) {
		return queryByPagesAndRevisions(mediaWikiBot.queryPagesWithRevisionByLinks(pageId, namespaces, IDS));
	}

	private Iterable<Revision> queryByPagesAndRevisions(Iterable<ParsedPage> pagesWithLatestsRevisions)
			throws ActionException, ProcessException {
		return queryByPagesAndRevisionsF().makeBatched(2048).apply(pagesWithLatestsRevisions);
	}

	private MultiresultFunction<ParsedPage, Revision> queryByPagesAndRevisionsF() throws ActionException,
			ProcessException {

		return new MultiresultFunction<ParsedPage, Revision>() {

			@Override
			public Iterable<Revision> apply(Iterable<? extends ParsedPage> pagesWithLatestsRevisions) {

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

					StoredRevision stored = storedRevisionDao.getRevisionById(getProject(), latestRevisionId);
					if (isCacheRecordValid(stored)) {
						resultMap.put(latestRevisionId, stored);
					} else {
						toLoad.add(latestRevisionId);
					}
				}

				for (ParsedRevision parsedRevision : mediaWikiBot.queryRevisionsByRevisionIdsF(true, CACHED).apply(
						toLoad)) {
					// update cache
					StoredRevision storedRevision = storedRevisionDao.getOrCreate(getProject(), parsedRevision);
					resultMap.put(storedRevision.getId(), storedRevision);
					updateUserCache(storedRevision);
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

	public Iterable<Revision> queryByRecentChanges(Direction direction, Date start) {
		return queryByPagesAndRevisions(mediaWikiBot.queryPagesWithRevisionByRecentChanges(direction, start,
				"edit|new", true, IDS));
	}

	public Iterable<Revision> queryLatestByPageIds(Iterable<Long> pageIds) {
		return queryByPagesAndRevisions(mediaWikiBot.queryLatestRevisionsByPageIds(pageIds, IDS));
	}

	public Iterable<Revision> queryLatestByPageTitles(Iterable<String> pageTitles, boolean followRedirects)
			throws ActionException, ProcessException {
		return queryByPagesAndRevisions(mediaWikiBot.queryLatestRevisionsByPageTitles(pageTitles, followRedirects, IDS));
	}

	@Transactional(propagation = Propagation.NEVER)
	public Revision queryLatestRevision(Long pageId) {
		Revision latest = mediaWikiBot.queryLatestRevision(pageId, IDS);
		if (latest == null)
			return null;
		return queryRevision(latest);
	}

	@Transactional(propagation = Propagation.NEVER)
	public Revision queryLatestRevision(String pageTitle) {
		Revision latest = mediaWikiBot.queryLatestRevision(pageTitle, false, IDS);
		if (latest == null)
			return null;
		return queryRevision(latest);
	}

	@Transactional(propagation = Propagation.NEVER)
	public StoredRevision queryRevision(Long revisionId) throws JwbfException {

		StoredRevision stored = storedRevisionDao.getRevisionById(getProject(), revisionId);
		if (isCacheRecordValid(stored)) {
			return stored;
		}

		Revision withContent = mediaWikiBot.queryRevisionByRevisionId(revisionId, false, CACHED);

		if (withContent == null)
			// deleted
			return null;

		StoredRevision revision = storedRevisionDao.getOrCreate(getProject(), withContent);
		updateUserCache(revision);
		return revision;
	}

	@Transactional(propagation = Propagation.NEVER)
	public StoredRevision queryRevision(Revision revision) {
		if (revision == null) {
			throw new IllegalArgumentException("revision");
		}
		if (revision instanceof StoredRevision) {
			return ((StoredRevision) revision);
		}
		return queryRevision(revision.getId());
	}

	public Iterable<Revision> queryRevisions(Iterable<? extends Revision> revisions) {
		return queryRevisionsImplF().apply(revisions);
	}

	private MultiresultFunction<Revision, Revision> queryRevisionsImplF() {
		return new MultiresultFunction<Revision, Revision>() {
			@Override
			public Iterable<Revision> apply(Iterable<? extends Revision> revisionIdHolders) {
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

				Iterable<ParsedRevision> revisionsWithContent = mediaWikiBot.queryRevisionsByRevisionIdsF(true, CACHED)
						.apply(toLoad);
				// update cache
				for (Revision revision : storedRevisionDao.getOrCreate(getProject(), revisionsWithContent)) {
					resultMap.put(revision.getId(), revision);
					updateUserCache(revision);
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
		}.makeBatched(1000);
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

	protected void updateUserCache(Revision revision) {
		// update user cache as well
		if (revision.getUserId() != null && revision.getUserId().longValue() != 0
				&& StringUtils.isNotEmpty(revision.getUser())) {
			ParsedUser parsedUser = new ParsedUser();
			parsedUser.setUserId(revision.getUserId());
			parsedUser.setName(revision.getUser());
			storedUserDao.getOrCreate(getProject(), parsedUser);
		}
	}

}
