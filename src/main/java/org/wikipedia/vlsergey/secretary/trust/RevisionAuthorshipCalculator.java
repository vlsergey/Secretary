package org.wikipedia.vlsergey.secretary.trust;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespaces;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.LastUserHashMap;
import org.wikipedia.vlsergey.secretary.webcite.RefAwareParser;

public class RevisionAuthorshipCalculator {

	private class PageContext {

		final LastUserHashMap<Long, TextChunkList> anonymChunksCache = new LastUserHashMap<Long, TextChunkList>(
				CONTEXT_CACHES_SIZE);

		final String pageTitle;

		final LastUserHashMap<Long, Revision> revisionCache = new LastUserHashMap<Long, Revision>(CONTEXT_CACHES_SIZE);

		final Map<Long, Long> revisionChunkedLength = new HashMap<Long, Long>();
		final Map<Long, Revision> revisionInfos = new HashMap<Long, Revision>();

		final List<Revision> revisionInfosNewer;
		final List<Long> revisionInfosNewerIds;

		final List<Revision> revisionInfosOlder;
		final List<Long> revisionInfosOlderIds;

		final int revisionInfosSize;

		public PageContext(String pageTitle) {
			this.pageTitle = pageTitle;

			this.revisionInfosNewer = mediaWikiBot.queryRevisionsByPageTitle(pageTitle, null, Direction.NEWER,
					WikiCache.FAST);
			this.revisionInfosNewerIds = new ArrayList<Long>(revisionInfosNewer.size());
			for (Revision revision : revisionInfosNewer) {
				revisionInfosNewerIds.add(revision.getId());
				revisionInfos.put(revision.getId(), revision);
			}

			this.revisionInfosOlder = new ArrayList<Revision>(revisionInfosNewer);
			this.revisionInfosOlderIds = new ArrayList<Long>(revisionInfosNewerIds);
			Collections.reverse(revisionInfosOlder);
			Collections.reverse(revisionInfosOlderIds);

			this.revisionInfosSize = revisionInfosNewer.size();
		}

		synchronized TextChunkList getAnonymChunks(Long revisionId) {
			TextChunkList chunks = anonymChunksCache.get(revisionId);
			if (chunks == null) {
				chunks = TextChunkList
						.toTextChunkList(getLocale(), "127.0.0.1", queryRevision(revisionId).getContent());
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

		public Revision queryRevisionInfo(Long revisionId) {
			return revisionInfos.get(revisionId);
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
				return revisionInfosNewer.subList(index, revisionInfosSize);
			}
			case OLDER: {
				if (rvStartId == null) {
					return revisionInfosOlder;
				}
				final int index = revisionInfosOlderIds.indexOf(rvStartId);
				if (index == -1) {
					throw new IllegalArgumentException("Unknown revision: " + rvStartId + " of '" + pageTitle + "'");
				}
				return revisionInfosOlder.subList(index, revisionInfosSize);
			}
			default:
				throw new AssertionError();
			}
		}

	}

	private static final int CONTEXT_CACHES_SIZE = 1000;

	private static final Logger log = LoggerFactory.getLogger(RevisionAuthorshipCalculator.class);

	private static final boolean PRELOAD = true;

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

	// private final ExecutorService executor =
	// Executors.newSingleThreadExecutor();
	private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

	private Locale locale;

	private MediaWikiBot mediaWikiBot;

	private final ExecutorService preloadExecutor = Executors.newFixedThreadPool(Runtime.getRuntime()
			.availableProcessors());

	private RefAwareParser refAwareParser;

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

	public TextChunkList getAuthorship(Page page, Revision latestRevisionContent, final Date lastPossibleEditTimestamp)
			throws Exception {

		final int lockIndex = (int) ((page.getId().longValue() % SEGMENTS + SEGMENTS) % SEGMENTS);
		final Lock lock = articleLocks[lockIndex];
		lock.lock();
		try {
			return getAuthorshipImpl(page, latestRevisionContent, lastPossibleEditTimestamp);
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
			final TextChunkList authorship = TextChunkList.toTextChunkList(getLocale(), newRevision.getUser(),
					newRevision.getContent());
			revisionAuthorshipDao.store(getLocale(), newRevision, authorship);
			return authorship;
		}

		// found
		final TextChunkList oldAuthorship = getAuthorship(pageContext, oldRevisionId);

		final Revision oldRevision = pageContext.queryRevision(oldRevisionId);
		final Revision newRevision = pageContext.queryRevision(newRevisionId);

		log.info("Get authorwhip for " + toString(newRevision) + ": calculate basing on difference with "
				+ toString(oldRevision));
		TextChunkList newAuthorship = TextChunkList.toTextChunkList(getLocale(), newRevision.getUser(),
				newRevision.getContent());
		newAuthorship = join(oldAuthorship, newAuthorship);
		revisionAuthorshipDao.store(getLocale(), newRevision, newAuthorship);
		return newAuthorship;
	}

	private TextChunkList getAuthorshipFromDatabase(final PageContext pageContext, final Long newRevisionId) {
		return getAuthorshipFromDatabaseImpl(newRevisionId, new Callable<Revision>() {

			@Override
			public Revision call() throws Exception {
				return pageContext.queryRevisionInfo(newRevisionId);
			}
		}, new Callable<String>() {

			@Override
			public String call() throws Exception {
				return pageContext.queryRevision(newRevisionId).getContent();
			}
		});
	}

	private TextChunkList getAuthorshipFromDatabase(final Revision revisionContent) {
		return getAuthorshipFromDatabaseImpl(revisionContent.getId(), new Callable<Revision>() {

			@Override
			public Revision call() throws Exception {
				return revisionContent;
			}
		}, new Callable<String>() {

			@Override
			public String call() throws Exception {
				return revisionContent.getContent();
			}
		});
	}

	private TextChunkList getAuthorshipFromDatabaseImpl(Long newRevisionId, Callable<Revision> revisionInfoF,
			Callable<String> contentF) {
		try {
			final RevisionAuthorship stored = revisionAuthorshipDao.findByRevision(getLocale(), newRevisionId);
			if (stored != null && stored.getData() != null && stored.getData().length != 0) {
				TextChunkList restored = TextChunkList.fromBinary(getLocale(), contentF.call(), stored.getData());
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

	private TextChunkList getAuthorshipImpl(Page page, Revision latestRevisionContent,
			final Date lastPossibleEditTimestamp) throws Exception, InterruptedException, ExecutionException {
		if (lastPossibleEditTimestamp == null && latestRevisionContent != null) {
			// check DB before preloading
			TextChunkList chunks = getAuthorshipFromDatabase(latestRevisionContent);
			if (chunks != null) {
				return chunks;
			}
		}

		{
			Long toLookupInCache;
			if (lastPossibleEditTimestamp == null) {
				/*
				 * если контрольная дата не установлена -- значит изучаем
				 * последнюю версию
				 */
				toLookupInCache = latestRevisionContent != null ? latestRevisionContent.getId() : null;
			} else if (latestRevisionContent.getTimestamp() != null
					&& latestRevisionContent.getTimestamp().before(lastPossibleEditTimestamp)) {
				/*
				 * Если текущая последняя версия сделана до контрольной даты,
				 * значит её и нужно изучать
				 */
				toLookupInCache = latestRevisionContent.getId();
			} else {
				/*
				 * Нужно узнать, какая именно версия сделана ДО установленной
				 * контрольной даты (если это не текущая последняя). Может быть,
				 * у нас есть эта информация в кеше?
				 */
				ToDateArticleRevision revision = toDateArticleRevisionDao.find(getLocale(), page,
						lastPossibleEditTimestamp);
				toLookupInCache = revision != null ? revision.getRevisionId() : null;
			}

			if (toLookupInCache != null) {
				TextChunkList chunks = getAuthorshipFromDatabase(wikiCache.queryRevision(toLookupInCache));
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
				toDateArticleRevisionDao.store(getLocale(), page, lastPossibleEditTimestamp,
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

		if (PRELOAD) {
			// pregenerate to prevent out of memory and out of stack?
			log.info("Preload all revisions of '" + pageTitle + "'");

			// preload all revisions
			final PageContext pageContext2 = pageContext;
			List<Future<?>> futures = new ArrayList<Future<?>>();
			for (final Revision revision : wikiCache.queryRevisions(pageContext.queryRevisionsInfo(null,
					Direction.NEWER))) {
				futures.add(preloadExecutor.submit(new Runnable() {
					@Override
					public void run() {
						final TextChunkList chunks = TextChunkList.toTextChunkList(getLocale(), "127.0.0.1",
								revision.getContent());
						synchronized (pageContext2) {
							pageContext2.anonymChunksCache.put(revision.getId(), chunks);
							pageContext2.revisionChunkedLength.put(revision.getId(), Long.valueOf(chunks.length()));
							pageContext2.revisionCache.put(revision.getId(), revision);
						}
					}
				}));
			}

			for (Future<?> future : futures) {
				future.get();
			}
		}

		if (pageContext.revisionInfosOlderIds.size() > 100) {
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
			}
		}

		return getAuthorship(pageContext, revisionToResearchId.getId());
	}

	public Locale getLocale() {
		return locale;
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public RefAwareParser getRefAwareParser() {
		return refAwareParser;
	}

	private Long getRevisionToCompareWith(PageContext pageContext, Long newRevisionId) {

		if (newRevisionId == null) {
			throw new NullArgumentException("newRevisionId");
		}

		final long newRevisionLength = pageContext.revisionChunkedLength.get(newRevisionId);
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

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setRefAwareParser(RefAwareParser refAwareParser) {
		this.refAwareParser = refAwareParser;
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
		for (final Revision latestRevisionIdContent : wikiCache.queryLatestContentByPageIds(mediaWikiBot
				.queryEmbeddedInPageIds(template, Namespaces.MAIN))) {

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

	private void write(String title, SortedMap<String, TextChunkList> results) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : results.keySet()) {
			stringBuilder.append("* [[" + key + "]]: " + toString(results.get(key), true) + "\n");
		}

		mediaWikiBot.writeContent("User:" + mediaWikiBot.getLogin() + "/" + title, null, stringBuilder.toString(),
				null, "Обновление статистики", true, false);
	}
}
