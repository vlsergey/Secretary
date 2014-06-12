package org.wikipedia.vlsergey.secretary.trust;

import gnu.trove.map.TLongIntMap;
import gnu.trove.map.hash.TLongIntHashMap;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang.NullArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.LastUserHashMap;

public class RevisionAuthorshipCalculator {

	private class PageContext {

		final LastUserHashMap<Long, TextChunkList> anonymChunksCache = new LastUserHashMap<Long, TextChunkList>(
				CONTEXT_CACHES_SIZE);

		final Page page;
		final Long pageId;
		final String pageTitle;

		final LastUserHashMap<Long, Revision> revisionCache = new LastUserHashMap<Long, Revision>(CONTEXT_CACHES_SIZE);

		final TLongIntMap revisionChunkedLength = new TLongIntHashMap();
		final Map<Long, Revision> revisionInfos = new HashMap<Long, Revision>();

		final List<Revision> revisionInfosNewer;
		final List<Long> revisionInfosNewerIds;

		final List<Revision> revisionInfosOlder;
		final List<Long> revisionInfosOlderIds;

		final int revisionsCount;

		public PageContext(String pageTitle) {
			this.pageTitle = pageTitle;

			this.revisionInfosNewer = mediaWikiBot.queryRevisionsByPageTitle(pageTitle, null, Direction.NEWER,
					WikiCache.FAST);
			this.revisionInfosNewerIds = new ArrayList<Long>(revisionInfosNewer.size());
			for (Revision revision : revisionInfosNewer) {
				revisionInfosNewerIds.add(revision.getId());
				revisionInfos.put(revision.getId(), revision);
			}

			this.revisionsCount = revisionInfosNewer.size();

			if (revisionsCount != 0) {
				page = revisionInfosNewer.get(0).getPage();
				pageId = page.getId();
			} else {
				page = null;
				pageId = null;
			}

			this.revisionInfosOlder = new ArrayList<Revision>(revisionInfosNewer);
			this.revisionInfosOlderIds = new ArrayList<Long>(revisionInfosNewerIds);
			Collections.reverse(revisionInfosOlder);
			Collections.reverse(revisionInfosOlderIds);

		}

		synchronized TextChunkList getAnonymChunks(Long revisionId) {
			TextChunkList chunks = anonymChunksCache.get(revisionId);
			if (chunks == null) {
				chunks = TextChunkList.toTextChunkList(getProject().getLocale(), "127.0.0.1", queryRevision(revisionId)
						.getContent());
				anonymChunksCache.put(revisionId, chunks);
			}
			return chunks;
		}

		Revision getRevisionInfoJustBefore(Date borderDate) {
			for (Revision candidate : queryRevisionsInfo(null, Direction.OLDER)) {
				if (candidate.getTimestamp().before(borderDate)) {
					return candidate;
				}
			}
			return null;
		}

		public Revision queryLatestRevision() {
			return queryRevision(revisionInfosOlderIds.get(0));
		}

		public Revision queryLatestRevisionInfo() {
			return revisionInfosOlder.get(0);
		}

		public synchronized Revision queryRevision(Long revisionId) {
			if (revisionId == null) {
				throw new NullArgumentException("revisionId");
			}
			Revision revision = revisionCache.get(revisionId);
			if (revision == null) {
				revision = wikiCache.queryRevision(revisionId);
				revisionCache.put(revisionId, revision);
			}
			return revision;
		}

		public Iterable<Revision> queryRevisionsInfo(Long rvStartId, Direction direction) {
			switch (direction) {
			case NEWER: {
				if (rvStartId == null) {
					return revisionInfosNewer;
				}
				final int index = revisionInfosNewerIds.indexOf(rvStartId);
				if (index == -1) {
					throw new IllegalArgumentException("Unknown revision: " + rvStartId + " of '" + pageTitle + "'");
				}
				return revisionInfosNewer.subList(index, revisionsCount);
			}
			case OLDER: {
				if (rvStartId == null) {
					return revisionInfosOlder;
				}
				final int index = revisionInfosOlderIds.indexOf(rvStartId);
				if (index == -1) {
					throw new IllegalArgumentException("Unknown revision: " + rvStartId + " of '" + pageTitle + "'");
				}
				return revisionInfosOlder.subList(index, revisionsCount);
			}
			default:
				throw new AssertionError();
			}
		}

		public synchronized Revision queryRevisionWithPreload(Long revisionId, Direction direction, int preload) {
			if (revisionId == null) {
				throw new NullArgumentException("revisionId");
			}
			Revision revision = revisionCache.get(revisionId);
			if (revision == null) {

				final List<Revision> sublist;
				if (direction == Direction.NEWER) {
					sublist = sublistSafe(revisionInfosNewer, revisionInfos.get(revisionId), preload);
				} else {
					sublist = sublistSafe(revisionInfosOlder, revisionInfos.get(revisionId), preload);
				}
				for (Revision revisionContent : wikiCache.queryRevisions(sublist)) {
					revisionCache.put(revisionId, revisionContent);
				}

				revisionCache.put(revisionId, revision);
			}
			return revision;
		}

		private <T> List<T> sublistSafe(List<T> original, T start, int length) {
			int index = original.indexOf(start);
			if (index == -1) {
				throw new IllegalArgumentException("Start item " + start + " not present in full list");
			}
			int end = Math.min(index + length, original.size());
			return original.subList(index, end);
		}

	}

	private static final int CONTEXT_CACHES_SIZE = 1000;

	private static final Logger log = LoggerFactory.getLogger(RevisionAuthorshipCalculator.class);

	public static final int PRELOAD_BATCH = 25;

	public static final int SEGMENTS = 256;

	public static String concatenate(Iterable<TextChunk> chunks, String delimeter) {
		StringBuilder result = new StringBuilder();
		for (TextChunk chunk : chunks) {
			result.append(chunk.text);
			result.append(delimeter);
		}
		return result.toString();
	}

	public static String concatenate(TextChunk[] chunks, String delimeter) {
		StringBuilder result = new StringBuilder();
		for (TextChunk chunk : chunks) {
			result.append(chunk.text);
			result.append(delimeter);
		}
		return result.toString();
	}

	static String toString(TextChunkList authorship, boolean wiki) {

		LinkedHashMap<String, Double> result = authorship.getAuthorshipProcents();

		int count = 0;
		StringBuilder stringBuilder = new StringBuilder();
		for (String userName : result.keySet()) {
			double procent = result.get(userName).doubleValue();
			if (count >= 5 && procent < .05) {
				break;
			}
			count++;
			final String strProcent = new DecimalFormat("###.##").format(procent * 100) + "%";
			if (wiki) {
				stringBuilder.append("[[User:" + userName + "|" + userName + "]] " + strProcent + "; ");
			} else {
				stringBuilder.append(userName + " " + strProcent + "; ");
			}
		}

		return stringBuilder.toString().trim();
	}

	private final Lock[] articleLocks;

	@Autowired
	private PageRevisionChunksLengthDao chunksLengthDao;

	// private final ExecutorService executor =
	// Executors.newSingleThreadExecutor();
	private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	private MediaWikiBot mediaWikiBot;

	private Project project;

	@Autowired
	private RevisionAuthorshipDao revisionAuthorshipDao;

	@Autowired
	private ToDateArticleRevisionDao toDateArticleRevisionDao;

	private WikiCache wikiCache;

	public RevisionAuthorshipCalculator() {
		articleLocks = new Lock[SEGMENTS];
		for (int i = 0; i < articleLocks.length; i++) {
			articleLocks[i] = new ReentrantLock();
		}
	}

	private long calculateDifference(PageContext pageContext, Long oldRevisionId, Long newRevisionId) {
		TextChunkList chunks1 = pageContext.getAnonymChunks(oldRevisionId);
		TextChunkList chunks2 = pageContext.getAnonymChunks(newRevisionId);
		return TextChunkList.calculateDifference(chunks1, chunks2);
	}

	private void cleanupCache(PageContext pageContext, Set<Long> preserveRevisionIds) {
		wikiCache.removePageRevisionsExcept(pageContext.page, preserveRevisionIds);
	}

	public TextChunkList getAuthorship(Page page, Revision latestRevisionInfo, final Date lastPossibleEditTimestamp)
			throws Exception {

		final int lockIndex = (int) ((page.getId().longValue() % SEGMENTS + SEGMENTS) % SEGMENTS);
		final Lock lock = articleLocks[lockIndex];
		lock.lock();
		try {
			return getAuthorshipImpl(page, latestRevisionInfo, lastPossibleEditTimestamp);
		} finally {
			lock.unlock();
		}
	}

	private TextChunkList getAuthorship(PageContext pageContext, Long newRevisionId) throws Exception {
		if (newRevisionId == null) {
			throw new NullArgumentException("newRevisionId");
		}

		log.info("Get authorwhip for rev#" + newRevisionId);

		TextChunkList chunks = getAuthorshipFromDatabase(pageContext, newRevisionId);
		if (chunks != null) {
			return chunks;
		}

		// not found
		Long oldRevisionId = getRevisionToCompareWith(pageContext, newRevisionId);

		if (oldRevisionId == null) {
			// this is the first
			final Revision newRevision = pageContext.queryRevision(newRevisionId);
			final TextChunkList authorship = TextChunkList.toTextChunkList(getProject().getLocale(),
					newRevision.getUser(), newRevision.getContent());
			revisionAuthorshipDao.store(getProject(), newRevision, authorship);
			return authorship;
		}

		// found
		final TextChunkList oldAuthorship = getAuthorship(pageContext, oldRevisionId);

		final Revision oldRevision = pageContext.queryRevision(oldRevisionId);
		final Revision newRevision = pageContext.queryRevision(newRevisionId);

		log.info("Get authorwhip for " + toString(newRevision) + ": calculate basing on difference with "
				+ toString(oldRevision));
		TextChunkList newAuthorship = TextChunkList.toTextChunkList(getProject().getLocale(), newRevision.getUser(),
				newRevision.getContent());
		newAuthorship = join(oldAuthorship, newAuthorship);
		revisionAuthorshipDao.store(getProject(), newRevision, newAuthorship);
		return newAuthorship;
	}

	private TextChunkList getAuthorshipFromDatabase(final PageContext pageContext, final Long newRevisionId) {
		return getAuthorshipFromDatabaseImpl(newRevisionId, new Callable<String>() {
			@Override
			public String call() throws Exception {
				return pageContext.queryRevision(newRevisionId).getContent();
			}
		});
	}

	private TextChunkList getAuthorshipFromDatabaseImpl(Long newRevisionId, Callable<String> contentF) {
		try {
			final RevisionAuthorship stored = revisionAuthorshipDao.findByRevision(getProject(), newRevisionId);
			if (stored != null && stored.getData() != null && stored.getData().length != 0) {
				TextChunkList restored = TextChunkList.fromBinary(getProject().getLocale(), contentF.call(),
						stored.getData());
				if (restored != null) {
					log.debug("Get authorwhip for rev#" + newRevisionId + ": found in DB");
					return restored;
				}
			}
		} catch (Exception exc) {
			log.debug("Not good stored one: " + exc, exc);
		}
		return null;
	}

	private TextChunkList getAuthorshipImpl(Page page, final Revision latestRevisionInfo,
			final Date lastPossibleEditTimestamp) throws Exception, InterruptedException, ExecutionException {

		if (latestRevisionInfo.getTimestamp() != null
				&& (lastPossibleEditTimestamp == null || latestRevisionInfo.getTimestamp().before(
						lastPossibleEditTimestamp))) {
			/*
			 * если контрольная дата не установлена или если текущая последняя
			 * версия сделана до контрольной даты -- значит изучаем последнюю
			 * версию
			 */
			TextChunkList chunks = getAuthorshipFromDatabaseImpl(latestRevisionInfo.getId(), new Callable<String>() {
				@Override
				public String call() throws Exception {
					return wikiCache.queryRevision(latestRevisionInfo).getContent();
				}
			});
			if (chunks != null) {
				return chunks;
			}
		} else {
			/*
			 * Нужно узнать, какая именно версия сделана ДО установленной
			 * контрольной даты (если это не текущая последняя). Может быть, у
			 * нас есть эта информация в кеше?
			 */
			final ToDateArticleRevision toDateRevisionInfo = toDateArticleRevisionDao.find(getProject(), page,
					lastPossibleEditTimestamp);

			if (toDateRevisionInfo != null) {
				TextChunkList chunks = getAuthorshipFromDatabaseImpl(toDateRevisionInfo.getRevisionId(),
						new Callable<String>() {
							@Override
							public String call() throws Exception {
								return wikiCache.queryRevision(toDateRevisionInfo.getRevisionId()).getContent();
							}
						});
				if (chunks != null) {
					return chunks;
				}
			}
		}

		final String pageTitle = page.getTitle();
		final PageContext pageContext = new PageContext(pageTitle);
		if (pageContext.revisionInfosNewer.isEmpty()) {
			return null;
		}

		Revision revisionToResearchId = null;
		if (lastPossibleEditTimestamp == null) {
			revisionToResearchId = pageContext.queryLatestRevision();
		} else {
			revisionToResearchId = pageContext.getRevisionInfoJustBefore(lastPossibleEditTimestamp);

			if (revisionToResearchId != null
					&& !revisionToResearchId.equals(pageContext.queryLatestRevisionInfo().getId())) {
				toDateArticleRevisionDao.store(getProject(), page, lastPossibleEditTimestamp,
						revisionToResearchId.getId());
			}
		}

		if (revisionToResearchId == null) {
			return null;
		} else {
			TextChunkList chunks = getAuthorshipFromDatabase(pageContext, revisionToResearchId.getId());
			if (chunks != null) {
				return chunks;
			}
		}

		TLongIntMap chunkLengths = chunksLengthDao.findSafe(getProject(), pageContext.pageId);
		{
			// we need ALL revisions chunk lengths
			List<Revision> missingLengths = new ArrayList<Revision>(pageContext.revisionsCount - chunkLengths.size());
			for (Revision revisionInfo : pageContext.revisionInfosOlder) {
				if (!chunkLengths.containsKey(revisionInfo.getId().longValue())) {
					missingLengths.add(revisionInfo);
				}
			}

			if (!missingLengths.isEmpty()) {
				for (Revision revision : wikiCache.queryRevisions(missingLengths)) {
					final TextChunkList chunks = TextChunkList.toTextChunkList(getProject().getLocale(), "127.0.0.1",
							revision.getContent());

					chunkLengths.put(revision.getId(), chunks.length());

					// usually it is the latest ones...
					pageContext.anonymChunksCache.put(revision.getId(), chunks);
					pageContext.revisionCache.put(revision.getId(), revision);
				}
				chunksLengthDao.store(getProject(), pageContext.pageId, chunkLengths);
			}
		}
		pageContext.revisionChunkedLength.putAll(chunkLengths);

		Set<Long> preserveRevisionIds = new HashSet<Long>();
		preserveRevisionIds.add(pageContext.queryLatestRevisionInfo().getId());

		{
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.DATE, 1);
			calendar.set(Calendar.HOUR_OF_DAY, 0);
			calendar.set(Calendar.MINUTE, 0);
			calendar.set(Calendar.SECOND, 0);
			calendar.set(Calendar.MILLISECOND, 0);

			List<Revision> toPregenerate = new ArrayList<Revision>();
			Revision revisionToPregenerate;
			while ((revisionToPregenerate = pageContext.getRevisionInfoJustBefore(calendar.getTime())) != null) {
				log.debug("Add revision " + revisionToPregenerate + " as border revision for " + calendar.getTime());
				toPregenerate.add(revisionToPregenerate);

				calendar.add(Calendar.MONTH, -1);
			}
			Collections.reverse(toPregenerate);

			for (final Revision revisionInfo : toPregenerate) {
				getAuthorship(pageContext, revisionInfo.getId());
				preserveRevisionIds.add(revisionInfo.getId());
			}
		}

		{
			int counter = 10;
			for (Long revisionId : pageContext.revisionInfosOlderIds) {
				counter--;
				preserveRevisionIds.add(revisionId);
				if (counter == 0) {
					break;
				}
			}
		}

		TextChunkList result = getAuthorship(pageContext, revisionToResearchId.getId());
		cleanupCache(pageContext, preserveRevisionIds);
		return result;
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public Project getProject() {
		return project;
	}

	private Long getRevisionToCompareWith(PageContext pageContext, Long newRevisionId) {

		if (newRevisionId == null) {
			throw new NullArgumentException("newRevisionId");
		}

		final int newRevisionLength = pageContext.revisionChunkedLength.get(newRevisionId);
		final Revision newRevision = pageContext.queryRevision(newRevisionId);
		log.info("Searching through history for best compare candidate for " + toString(newRevision));

		Revision candidate = null;
		long minDiffrence = Integer.MAX_VALUE;

		/*
		 * Просматриваем все ревизии от старой до новой с целью найти наименьшее
		 * изменение в тексте по сравнению с текущей ревизией
		 */
		for (Revision oldRevision : pageContext.queryRevisionsInfo(newRevisionId, Direction.OLDER)) {

			if (oldRevision.getId().equals(newRevision.getId())) {
				continue;
			}

			final long oldRevisionLength = pageContext.revisionChunkedLength.get(oldRevision.getId());

			if (Math.abs(newRevisionLength - oldRevisionLength) > minDiffrence + 5) {
				// not a candidate for sure
				continue;
			}

			log.debug("Searching through history for best compare candidate for " + toString(newRevision)
					+ ": compare with " + toString(oldRevision));

			pageContext.queryRevisionWithPreload(oldRevision.getId(), Direction.OLDER, PRELOAD_BATCH);

			long difference = calculateDifference(pageContext, oldRevision.getId(), newRevisionId);

			if (difference < minDiffrence) {
				minDiffrence = difference;
				candidate = oldRevision;

				log.debug("Searching through history for best compare candidate for " + toString(newRevision)
						+ ": minimal difference changed to " + minDiffrence);
			}
		}

		log.info("Searching through history for best compare candidate for " + toString(newRevision)
				+ ": best candidate is " + candidate);

		return candidate == null ? null : candidate.getId();
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	public TextChunkList join(TextChunkList baseRevisionOriginal, TextChunkList newRevisionOriginal) {
		TextChunkList baseRevision = baseRevisionOriginal;

		BitSet done = new BitSet();
		List<TextChunkList> newRevisionParts = new ArrayList<TextChunkList>();
		newRevisionParts.add(newRevisionOriginal);

		while (true) {

			TextChunkList longestCommon = TextChunkList.EMPTY;

			for (int i = 0; i < newRevisionParts.size(); i++) {
				if (done.get(i)) {
					continue;
				}

				TextChunkList newRevisionPart = newRevisionParts.get(i);
				if (newRevisionPart.size() <= longestCommon.size()) {
					// no chance to find longer
					continue;
				}

				TextChunkList candidate = TextChunkList.lcs(baseRevision, newRevisionPart);
				if (longestCommon.size() < candidate.size()) {
					// candidate is always part of baseRevision (LCS contract)
					longestCommon = candidate;
				}
			}

			if (longestCommon.isEmpty()) {
				break;
			}

			BitSet newDone = new BitSet();
			List<TextChunkList> newParts = new ArrayList<TextChunkList>();

			for (int i = 0; i < newRevisionParts.size(); i++) {
				final TextChunkList newRevisionPart = newRevisionParts.get(i);

				if (done.get(i)) {
					newDone.set(newParts.size(), true);
					newParts.add(newRevisionPart);
					continue;
				}

				int index = newRevisionPart.indexOf(longestCommon);

				if (index == -1) {
					newDone.set(newParts.size(), false);
					newParts.add(newRevisionPart);
					continue;
				}

				if (index == 0) {

					// 1st part
					newDone.set(newParts.size(), true);
					newParts.add(longestCommon);

					if (longestCommon.size() != newRevisionPart.size()) {
						// 2 parts
						TextChunkList rest = newRevisionPart.subList(longestCommon.size(), newRevisionPart.size());
						newDone.set(newParts.size(), false);
						newParts.add(rest);
					}

				} else {

					// prefix
					{
						TextChunkList prefix = newRevisionPart.subList(0, index);
						newDone.set(newParts.size(), false);
						newParts.add(prefix);
					}

					// lcs part
					newDone.set(newParts.size(), true);
					newParts.add(longestCommon);

					// suffix?
					if (index + longestCommon.size() < newRevisionPart.size()) {
						TextChunkList suffix = newRevisionPart.subList(index + longestCommon.size(),
								newRevisionPart.size());
						newDone.set(newParts.size(), false);
						newParts.add(suffix);
					}

				}
			}

			newRevisionParts = newParts;
			done = newDone;

			// remove lcs from base revision
			baseRevision = baseRevision.remove(longestCommon);
		}

		TextChunkList newRevision = TextChunkList.concatenate(newRevisionParts);
		return newRevision;
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}

	private String toString(Revision revision) {
		return "rev#" + revision.getId() + " (" + revision.getTimestamp() + "; " + revision.getSize() + ")";
	}

	public void updateBlockCodes() {
		updateByTemplateIncluded("Авторство статей о блочных шифрах", "Шаблон:Карточка блочного шифра");
	}

	private void updateByTemplateIncluded(final String statPageTitle, final String template) {

		final SortedMap<String, TextChunkList> results = Collections
				.synchronizedSortedMap(new TreeMap<String, TextChunkList>());

		List<Future<TextChunkList>> futures = new ArrayList<Future<TextChunkList>>();
		for (final Revision latestRevisionIdContent : wikiCache.queryByEmbeddedIn(template,
				new Namespace[] { Namespace.MAIN })) {

			final Page page = latestRevisionIdContent.getPage();
			final String pageTitle = page.getTitle();

			futures.add(executor.submit(new Callable<TextChunkList>() {

				@Override
				public TextChunkList call() throws Exception {
					try {
						TextChunkList chunks = getAuthorship(page, latestRevisionIdContent, null);
						results.put(pageTitle, chunks);
						return chunks;
					} catch (Exception exc) {
						log.warn("Unable to calculate authorship for page '" + pageTitle + "': " + exc, exc);
						throw exc;
					}
				}
			}));
		}

		for (Future<?> future : futures) {
			try {
				future.get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}

		write(statPageTitle, results);
	}

	public void updateFeaturedArticles() {
		updateByTemplateIncluded("Авторство избранных статей", "Шаблон:Избранная статья");
	}

	public void updateGoodArticles() {
		updateByTemplateIncluded("Авторство хороших статей", "Шаблон:Хорошая статья");
	}

	public void updateQualityArticles() {
		updateByTemplateIncluded("Авторство добротных статей", "Шаблон:Добротная статья");
	}

	private void write(String title, SortedMap<String, TextChunkList> results) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : results.keySet()) {
			stringBuilder.append("* [[" + key + "]]: " + toString(results.get(key), true) + "\n");
		}

		stringBuilder.append("\n");
		stringBuilder.append("[[Категория:Википедия:Рейтинги авторов]]\n");

		mediaWikiBot.writeContent("User:" + mediaWikiBot.getLogin() + "/" + title, null, stringBuilder.toString(),
				null, "Обновление статистики", true, false);
	}

}
