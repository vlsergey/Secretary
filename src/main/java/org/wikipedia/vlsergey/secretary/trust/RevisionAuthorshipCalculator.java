package org.wikipedia.vlsergey.secretary.trust;

import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

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
import org.wikipedia.vlsergey.secretary.trust.princeton.LCS;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;
import org.wikipedia.vlsergey.secretary.webcite.RefAwareParser;

public class RevisionAuthorshipCalculator {

	private class PageContext {

		final LastUserHashMap<Long, List<TextChunk>> anonymChunksCache = new LastUserHashMap<Long, List<TextChunk>>(
				CONTEXT_CACHES_SIZE);

		final String pageTitle;

		final LastUserHashMap<Long, Revision> revisionCache = new LastUserHashMap<Long, Revision>(CONTEXT_CACHES_SIZE);

		final Map<Long, Long> revisionChunkedLength = new HashMap<Long, Long>();

		final List<Revision> revisionInfosNewer;
		final List<Long> revisionInfosNewerIds;

		final List<Revision> revisionInfosOlder;
		final List<Long> revisionInfosOlderIds;

		final int revisionInfosSize;

		PageContext(PageContext pageContext, Set<Long> toRemove) {
			this.pageTitle = pageContext.pageTitle;

			List<Revision> revisionInfosNewer = new ArrayList<Revision>();
			for (Revision revision : pageContext.revisionInfosNewer) {
				if (!toRemove.contains(revision.getId())) {
					revisionInfosNewer.add(revision);
				}
			}

			this.revisionInfosNewer = revisionInfosNewer;
			this.revisionInfosNewerIds = new ArrayList<Long>(revisionInfosNewer.size());
			for (Revision revision : revisionInfosNewer)
				revisionInfosNewerIds.add(revision.getId());

			this.revisionInfosOlder = new ArrayList<Revision>(revisionInfosNewer);
			this.revisionInfosOlderIds = new ArrayList<Long>(revisionInfosNewerIds);
			Collections.reverse(revisionInfosOlder);
			Collections.reverse(revisionInfosOlderIds);

			this.revisionInfosSize = revisionInfosNewer.size();
			this.revisionChunkedLength.putAll(pageContext.revisionChunkedLength);
		}

		public PageContext(String pageTitle) {
			this.pageTitle = pageTitle;

			this.revisionInfosNewer = mediaWikiBot.queryRevisionsByPageTitle(pageTitle, null, Direction.NEWER,
					WikiCache.FAST);
			this.revisionInfosNewerIds = new ArrayList<Long>(revisionInfosNewer.size());
			for (Revision revision : revisionInfosNewer)
				revisionInfosNewerIds.add(revision.getId());

			this.revisionInfosOlder = new ArrayList<Revision>(revisionInfosNewer);
			this.revisionInfosOlderIds = new ArrayList<Long>(revisionInfosNewerIds);
			Collections.reverse(revisionInfosOlder);
			Collections.reverse(revisionInfosOlderIds);

			this.revisionInfosSize = revisionInfosNewer.size();
		}

		List<TextChunk> getAnonymChunks(Long revisionId) {
			List<TextChunk> chunks = anonymChunksCache.get(revisionId);
			if (chunks == null) {
				chunks = toChunks("127.0.0.1", queryRevision(revisionId).getContent());
				anonymChunksCache.put(revisionId, chunks);
			}
			return chunks;
		}

		List<TextChunk> getAnonymChunks(Revision withContent) {
			List<TextChunk> chunks = anonymChunksCache.get(withContent.getId());
			if (chunks == null) {
				chunks = toChunks("127.0.0.1", withContent.getContent());
				anonymChunksCache.put(withContent.getId(), chunks);
			}
			return chunks;
		}

		public Revision queryLatestRevision() {
			return queryRevision(revisionInfosOlderIds.get(0));
		}

		public Revision queryLatestRevisionInfo() {
			return revisionInfosOlder.get(0);
		}

		public Revision queryRevision(Long revisionId) {
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

	private static class StringHash {

		private byte[] hash;

		StringHash(List<TextChunk> chunks) throws Exception {
			TextChunk[] sorted = chunks.toArray(new TextChunk[chunks.size()]);
			Arrays.parallelSort(sorted);

			String concatenated = concatenate(sorted, " ");
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			this.hash = md.digest(concatenated.getBytes("utf-8"));
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StringHash other = (StringHash) obj;
			if (!Arrays.equals(hash, other.hash))
				return false;
			return true;
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(hash);
		}

	}

	private static final Comparator<List<TextChunk>> COMPARATOR = new Comparator<List<TextChunk>>() {

		@Override
		public int compare(List<TextChunk> o1, List<TextChunk> o2) {
			int byFirst = o1.get(0).text.compareTo(o2.get(0).text);
			if (byFirst != 0) {
				return byFirst;
			}

			final int o1Size = o1.size();
			final int o2Size = o2.size();
			final int maxSize = Math.max(o1Size, o2Size);

			for (int i = 0; i < maxSize + 1; i++) {
				if (o1Size == i) {
					if (o2Size == i) {
						return 0;
					} else {
						return -1;
					}
				} else if (o2Size == i) {
					return +1;
				}

				int current = o1.get(i).text.compareTo(o2.get(i).text);
				if (current != 0) {
					return current;
				}
			}

			// shall not happen
			throw new AssertionError();
		}
	};

	private static final int CONTEXT_CACHES_SIZE = 100;

	private static final Logger log = LoggerFactory.getLogger(RevisionAuthorshipCalculator.class);

	private static final TextChunk NONPRESENT = new TextChunk("\0", "\0");

	private static final boolean PRECALCULATE = false;

	private static final boolean PRELOAD = true;

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

	public static LinkedHashMap<String, Double> getProcents(List<TextChunk> authorship) {
		final TObjectLongHashMap<String> byUsername = new TObjectLongHashMap<String>(16, 1, 0);

		long sum = 0;
		for (TextChunk textChunk : authorship) {
			final int value = textChunk.text.length();
			byUsername.put(textChunk.user, byUsername.get(textChunk.user) + value);
			sum += value;
		}

		List<String> userNames = new ArrayList<String>(byUsername.keySet());
		Collections.sort(userNames, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Long l1 = byUsername.get(o1);
				Long l2 = byUsername.get(o2);
				return l2.compareTo(l1);
			}
		});

		LinkedHashMap<String, Double> result = new LinkedHashMap<String, Double>();

		for (String userName : userNames) {
			long value = byUsername.get(userName);
			double procent = ((double) value) / sum;
			result.put(userName, Double.valueOf(procent));
		}

		return result;
	}

	private static <T> int indexOf(List<T> list, List<T> sublist) {
		final int max = list.size() - sublist.size() + 1;
		final T first = sublist.get(0);

		for (int i = 0; i < max; i++) {
			if (list.get(i).equals(first)) {
				if (list.subList(i, i + sublist.size()).equals(sublist)) {
					return i;
				}
			}
		}
		return -1;
	}

	private static <T> List<T> remove(List<T> list, List<T> sublist) {
		int index = indexOf(list, sublist);
		List<T> result = new ArrayList<T>();
		result.addAll(list.subList(0, index));
		result.addAll(list.subList(index + sublist.size(), list.size()));
		return result;
	}

	private static <T> void removeAllOnce(Collection<T> source, List<T> toRemove) {
		TObjectIntHashMap<T> counts = new TObjectIntHashMap<T>(toRemove.size(), 1, 0);
		for (T toRemoveItem : toRemove) {
			counts.put(toRemoveItem, counts.get(toRemoveItem) + 1);
		}

		for (Iterator<T> iterator = source.iterator(); iterator.hasNext();) {
			T toCheck = iterator.next();
			final int toDeleteCount = counts.get(toCheck);
			if (toDeleteCount > 0) {
				counts.put(toCheck, toDeleteCount - 1);
				iterator.remove();
			}
		}
	}

	static String toString(List<TextChunk> authorship, boolean wiki) {

		LinkedHashMap<String, Double> result = getProcents(authorship);

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

	private WikiCache wikiCache;

	private long calculateDifference(PageContext pageContext, Long oldRevisionId, Long newRevisionId) {
		List<TextChunk> chunks1 = pageContext.getAnonymChunks(oldRevisionId);
		List<TextChunk> chunks2 = pageContext.getAnonymChunks(newRevisionId);

		List<TextChunk> left1 = new LinkedList<TextChunk>(chunks1);
		List<TextChunk> left2 = new LinkedList<TextChunk>(chunks2);

		removeAllOnce(left1, chunks2);
		removeAllOnce(left2, chunks1);

		return length(left1) + length(left2);
	}

	public PageContext compact(PageContext pageContext) throws Exception {

		Set<Long> toCleanup = new HashSet<Long>();

		Map<StringHash, Long> byHash = new HashMap<StringHash, Long>();

		Revision prevInfo = null;
		List<TextChunk> prevChunk = null;

		for (Revision revision : wikiCache.queryRevisions(pageContext.queryRevisionsInfo(null, Direction.NEWER))) {
			List<TextChunk> currChunk = pageContext.getAnonymChunks(revision);

			try {
				if (prevInfo == null) {
					continue;
				}

				final StringHash currHash = new StringHash(currChunk);
				if (byHash.containsKey(currHash)) {
					log.debug("Compacting... Skip revision #" + revision.getId() + " by " + revision.getUser()
							+ " -- same content (may be with movings) as for " + byHash.get(currHash));
					toCleanup.add(revision.getId());
					continue;
				} else {
					byHash.put(currHash, revision.getId());
				}

				if (StringUtils.equals(prevInfo.getUser(), revision.getUser())) {
					// some optimization is possible...
					// do we have any removes?
					List<TextChunk> tempPrevChunk = new LinkedList<TextChunk>(prevChunk);
					removeAllOnce(tempPrevChunk, currChunk);
					if (tempPrevChunk.isEmpty()) {
						// no -- we can skip previous version
						toCleanup.add(prevInfo.getId());
						log.debug("Compacting... Skip revision #" + prevInfo.getId() + " by " + prevInfo.getUser()
								+ " -- only additions and movings");
						continue;
					}
				}

			} finally {
				prevInfo = revision;
				prevChunk = currChunk;
			}
		}

		log.info("Compacting " + toCleanup.size() + " revisions out of " + pageContext.revisionInfosSize + " for '"
				+ pageContext.pageTitle + "'");

		return new PageContext(pageContext, toCleanup);
	}

	private List<TextChunk> getAuthorship(PageContext pageContext, Long newRevisionId) throws Exception {
		if (newRevisionId == null) {
			throw new NullArgumentException("newRevisionId");
		}

		log.info("Get authorwhip for rev#" + newRevisionId);

		List<TextChunk> chunks = getAuthorshipFromDatabase(newRevisionId);
		if (chunks != null) {
			return chunks;
		}

		// not found
		Long oldRevisionId = getRevisionToCompareWith(pageContext, newRevisionId);

		if (oldRevisionId == null) {
			// this is the first
			final Revision newRevision = pageContext.queryRevision(newRevisionId);
			final List<TextChunk> authorship = toChunks(newRevision.getUser(), newRevision.getContent());
			revisionAuthorshipDao.store(getLocale(), newRevision, authorship);
			return authorship;
		}

		// found
		final List<TextChunk> oldAuthorship = getAuthorship(pageContext, oldRevisionId);

		final Revision oldRevision = pageContext.queryRevision(oldRevisionId);
		final Revision newRevision = pageContext.queryRevision(newRevisionId);

		log.info("Get authorwhip for " + toString(newRevision) + ": calculate basing on difference with "
				+ toString(oldRevision));
		List<TextChunk> newAuthorship = toChunks(newRevision.getUser(), newRevision.getContent());
		newAuthorship = join(oldAuthorship, newAuthorship);
		revisionAuthorshipDao.store(getLocale(), newRevision, newAuthorship);
		return newAuthorship;
	}

	public List<TextChunk> getAuthorship(String pageTitle, Revision latestRevisionIdHolder,
			final Date lastPossibleEditTimestamp) throws Exception {

		{
			// check DB before preloading
			List<TextChunk> chunks = getAuthorshipFromDatabase(latestRevisionIdHolder.getId());
			if (chunks != null) {
				return chunks;
			}
		}

		PageContext pageContext = new PageContext(pageTitle);
		if (pageContext.revisionInfosNewer.isEmpty()) {
			return null;
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
						final List<TextChunk> chunks = toChunks("127.0.0.1", revision.getContent());
						synchronized (pageContext2) {
							pageContext2.anonymChunksCache.put(revision.getId(), chunks);
							pageContext2.revisionChunkedLength.put(revision.getId(), Long.valueOf(length(chunks)));
							pageContext2.revisionCache.put(revision.getId(), revision);
						}
					}
				}));
			}

			for (Future<?> future : futures) {
				future.get();
			}
		}

		if (PRECALCULATE) {
			pageContext = compact(pageContext);

			log.info("Pregenerate authorship info for old versions of '" + pageTitle + "'");

			for (Revision revision : pageContext.queryRevisionsInfo(null, Direction.NEWER)) {
				getAuthorship(pageContext, revision.getId());
			}
		}

		Revision revision = null;
		if (lastPossibleEditTimestamp == null) {
			revision = pageContext.queryLatestRevision();
		} else {
			for (Revision candidate : pageContext.queryRevisionsInfo(null, Direction.OLDER)) {
				if (candidate.getTimestamp().before(lastPossibleEditTimestamp)) {
					revision = candidate;
					break;
				}
			}
		}

		if (revision == null) {
			return null;
		}
		return getAuthorship(pageContext, revision.getId());
	}

	private List<TextChunk> getAuthorshipFromDatabase(Long newRevisionId) {
		try {
			final RevisionAuthorship stored = revisionAuthorshipDao.findByRevision(getLocale(), newRevisionId);
			if (stored != null && stored.getData() != null && stored.getData().length != 0) {
				List<TextChunk> restored = RevisionAuthorshipDao.fromBinary(stored.getData());
				if (restored != null) {
					log.info("Get authorwhip for rev#" + newRevisionId + ": found in DB");
					return restored;
				}
			}
		} catch (Exception exc) {
			log.debug("Not good stored one: " + exc, exc);
		}
		return null;
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

	public List<TextChunk> join(List<TextChunk> baseRevisionOriginal, List<TextChunk> newRevisionOriginal) {
		List<TextChunk> baseRevision = new ArrayList<TextChunk>(baseRevisionOriginal);

		BitSet done = new BitSet();
		List<List<TextChunk>> newRevisionParts = new ArrayList<List<TextChunk>>();
		newRevisionParts.add(newRevisionOriginal);

		while (true) {

			List<TextChunk> longestCommon = Collections.emptyList();

			for (int i = 0; i < newRevisionParts.size(); i++) {
				if (done.get(i)) {
					continue;
				}

				List<TextChunk> newRevisionPart = newRevisionParts.get(i);
				if (newRevisionPart.size() <= longestCommon.size()) {
					// no chance to find longer
					continue;
				}

				List<TextChunk> candidate;
				if (newRevisionPart.size() == 1) {
					int index = baseRevision.indexOf(newRevisionPart.get(0));
					if (index == -1) {
						continue;
					}
					candidate = Collections.singletonList(baseRevision.get(index));
				} else {
					candidate = LCS.lcs(COMPARATOR, baseRevision, newRevisionPart, NONPRESENT);
					if (longestCommon.size() < candidate.size()) {
						// use chunk from baseRevision explicitly
						int index = indexOf(baseRevision, candidate);
						longestCommon = baseRevision.subList(index, index + candidate.size());
					}
				}
			}

			if (longestCommon.isEmpty()) {
				break;
			}

			BitSet newDone = new BitSet();
			List<List<TextChunk>> newParts = new ArrayList<List<TextChunk>>();

			for (int i = 0; i < newRevisionParts.size(); i++) {
				final List<TextChunk> newRevisionPart = newRevisionParts.get(i);

				if (done.get(i)) {
					newDone.set(newParts.size(), true);
					newParts.add(newRevisionPart);
					continue;
				}

				int index = indexOf(newRevisionPart, longestCommon);

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
						List<TextChunk> rest = newRevisionPart.subList(longestCommon.size(), newRevisionPart.size());
						newDone.set(newParts.size(), false);
						newParts.add(rest);
					}

				} else {

					// prefix
					{
						List<TextChunk> prefix = newRevisionPart.subList(0, index);
						newDone.set(newParts.size(), false);
						newParts.add(prefix);
					}

					// lcs part
					newDone.set(newParts.size(), true);
					newParts.add(longestCommon);

					// suffix?
					if (index + longestCommon.size() < newRevisionPart.size()) {
						List<TextChunk> suffix = newRevisionPart.subList(index + longestCommon.size(),
								newRevisionPart.size());
						newDone.set(newParts.size(), false);
						newParts.add(suffix);
					}

				}
			}

			newRevisionParts = newParts;
			done = newDone;

			// remove lcs from base revision
			baseRevision = remove(baseRevision, longestCommon);
		}

		List<TextChunk> newRevision = new ArrayList<TextChunk>();
		for (List<TextChunk> chunks : newRevisionParts) {
			newRevision.addAll(chunks);
		}

		return newRevision;
	}

	private long length(List<TextChunk> chunks) {
		long result = 0;
		for (TextChunk chunk : chunks) {
			result += chunk.text.length();
		}
		return result;
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

	public List<TextChunk> toChunks(String user, String text) {
		text = text.toLowerCase(getLocale());
		text = text.replaceAll("<\\s?\\/?[a-zA-Z ]+>", "");
		text = text.replace('ё', 'е');
		text = StringUtils.join(StringUtils.split(text, "(){}[]<>«»:;,.?!\'\"\\/ \t\r\n—|=_~#$%^&*+~`"), ' ');

		List<TextChunk> chunks = new ArrayList<TextChunk>();
		for (String word : text.split(" ")) {
			if (!StopWords.RUSSIAN.contains(word)) {
				chunks.add(new TextChunk(user, word.intern()));
			}
		}
		return chunks;
	}

	private String toString(Revision revision) {
		return "rev#" + revision.getId() + " (" + revision.getTimestamp() + "; " + revision.getSize() + ")";
	}

	public void updateBlockCodes() {
		updateByTemplateIncluded("Авторство статей о блочных шифрах", "Шаблон:Карточка блочного шифра", 10);
	}

	private void updateByTemplateIncluded(final String statPageTitle, final String template, final int updateAfter) {

		final AtomicInteger counter = new AtomicInteger(0);
		final SortedMap<String, List<TextChunk>> results = Collections
				.synchronizedSortedMap(new TreeMap<String, List<TextChunk>>());

		List<Future<List<TextChunk>>> futures = new ArrayList<Future<List<TextChunk>>>();
		for (final Revision latestRevisionIdHolder : mediaWikiBot.queryLatestRevisionsByPageIds(
				mediaWikiBot.queryEmbeddedInPageIds(template, Namespaces.MAIN), WikiCache.FAST)) {

			final Page page = latestRevisionIdHolder.getPage();
			final String pageTitle = page.getTitle();

			futures.add(executor.submit(new Callable<List<TextChunk>>() {

				@Override
				public List<TextChunk> call() throws Exception {
					try {
						List<TextChunk> chunks = getAuthorship(pageTitle, latestRevisionIdHolder, null);
						results.put(pageTitle, chunks);

						int done = counter.incrementAndGet();
						if (done % updateAfter == 0) {
							try {
								write(statPageTitle, results);
							} catch (Exception exc) {
								exc.printStackTrace();
							}
						}
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
		// already calculated
		updateByTemplateIncluded("Авторство избранных статей", "Шаблон:Избранная статья", 10000);
	}

	public void updateGoodArticles() {
		updateByTemplateIncluded("Авторство хороших статей", "Шаблон:Хорошая статья", 10);
	}

	private void write(String title, SortedMap<String, List<TextChunk>> results) {
		StringBuilder stringBuilder = new StringBuilder();
		for (String key : results.keySet()) {
			stringBuilder.append("* [[" + key + "]]: " + toString(results.get(key), true) + "\n");
		}

		mediaWikiBot.writeContent("User:" + mediaWikiBot.getLogin() + "/" + title, null, stringBuilder.toString(),
				null, "Обновление статистики", true, false);
	}
}
