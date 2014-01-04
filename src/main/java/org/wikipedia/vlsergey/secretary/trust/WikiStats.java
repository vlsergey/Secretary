package org.wikipedia.vlsergey.secretary.trust;

import gnu.trove.map.TObjectDoubleMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.math.stat.descriptive.SummaryStatistics;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespaces;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.User;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class WikiStats {

	private static final DecimalFormat decimalFormat = new DecimalFormat("###,##0.00");

	private static final DecimalFormat integerFormat = new DecimalFormat("###,##0");

	private static final Log log = LogFactory.getLog(WikiStats.class);

	private static final int MIN_PAGES_DIFFERENCE = 10000;

	private static <T> List<T> getKeysSortedByValueDesc(final TObjectDoubleHashMap<T> byUser) {
		List<T> keys = new ArrayList<T>(byUser.keySet());
		Collections.sort(keys, new Comparator<T>() {
			@Override
			public int compare(T o1, T o2) {
				Double l1 = byUser.get(o1);
				Double l2 = byUser.get(o2);
				return l2.compareTo(l1);
			}
		});
		return keys;
	}

	private final Map<StatisticsKey, TObjectIntMap<Month>> doneMonth = new HashMap<StatisticsKey, TObjectIntMap<Month>>();

	private Locale locale;

	private MediaWikiBot mediaWikiBot;

	private final Map<StatisticsKey, Map<Month, TObjectIntMap<Contributor>>> places = new HashMap<StatisticsKey, Map<Month, TObjectIntMap<Contributor>>>();

	private final Map<String, String> renamedUsers = new HashMap<String, String>();

	private RevisionAuthorshipCalculator revisionAuthorshipCalculator;

	private final ExecutorService totalStatsExecutor = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS,
			new LinkedBlockingQueue<Runnable>(), new CustomizableThreadFactory("TotalStats-"));

	private WikiCache wikiCache;

	{
		renamedUsers.put("BoBink", "InkBoB");
		renamedUsers.put("IzolL", "InkBoB");
		renamedUsers.put("TyyyVer", "InkBoB");

		renamedUsers.put("HiddeneN", "GorkyFromChe");
		renamedUsers.put("MonTheCeltics", "GorkyFromChe");

		renamedUsers.put("G8J", "Mggu77");

		renamedUsers.put("Nice big guy", "PavelUstinovich");

		renamedUsers.put("Panov1975", "Radimov");

		renamedUsers.put("Insuranze", "SkyBon");

		renamedUsers.put("Prozazhizni", "Smartass2006");
		renamedUsers.put("Smartass", "Smartass2006");
		renamedUsers.put("Zolumov", "Smartass2006");
		renamedUsers.put("ЛеонидовЕВ", "Smartass2006");

		renamedUsers.put("Uuuiiiccc", "Ua1-136-500");

		renamedUsers.put("Vlad Veschenikin", "Vlad Jursalim");
		renamedUsers.put("Vladislav Veschenikin", "Vlad Jursalim");
		renamedUsers.put("Владислав Вещеникин", "Vlad Jursalim");

		renamedUsers.put("Borrow-188", "X-Romix");
		renamedUsers.put("Бэримор", "X-Romix");

		renamedUsers.put("Батискаф обыкновенный", "Аурелиано Буэндиа");
		renamedUsers.put("Любитель грёзофарса", "Аурелиано Буэндиа");

		renamedUsers.put("Вам письмо", "Климова");

		renamedUsers.put("Ryadinsky Eugen", "Рядинский Евгений");
	}

	private List<Contributor> getAsContributors(final Map<String, List<Contributor>> allContributors, String userName) {
		List<Contributor> countFor;
		synchronized (allContributors) {
			countFor = allContributors.get(userName);
			if (countFor == null) {
				countFor = Collections.<Contributor> singletonList(new ContributorUser(userName));
				allContributors.put(userName, countFor);
			}
		}
		return countFor;
	}

	public Locale getLocale() {
		return locale;
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public RevisionAuthorshipCalculator getRevisionAuthorshipCalculator() {
		return revisionAuthorshipCalculator;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	private boolean isMonthDone(StatisticsKey statisticsKey, Month month, int already) {
		synchronized (doneMonth) {
			TObjectIntMap<Month> done = doneMonth.get(statisticsKey);
			if (done == null) {
				return false;
			}
			return done.get(month) >= already;
		}
	}

	private TObjectLongHashMap<String> loadCounters(final String statisticsFileName)
			throws UnsupportedEncodingException, FileNotFoundException, IOException {
		log.info("Loading articles statistics...");

		final TObjectLongHashMap<String> counters = new TObjectLongHashMap<String>(16, 1, 0);

		{
			LineNumberReader reader = new LineNumberReader(new InputStreamReader(
					new FileInputStream(statisticsFileName), "utf-8"));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					try {
						if (StringUtils.isBlank(line)) {
							continue;
						}
						String[] strings = StringUtils.split(line, "\t");
						final Long visits = Long.valueOf(strings[1]);
						counters.put(strings[0], visits);
					} catch (Exception exc) {
						System.out.println("Skip line: '" + StringEscapeUtils.escapeJava(line) + "'");
					}
				}
			} finally {
				reader.close();
			}
		}
		return counters;
	}

	private void loadTeam(final Map<String, List<Contributor>> nameToContributors, final String wikiGroupCode,
			final String teamName, final String teamDescription, boolean skipIncrementPlace, boolean removeOtherTeams) {
		log.info("Loading user group: '" + wikiGroupCode + "'");
		TreeSet<String> userNames = new TreeSet<String>();
		for (User user : mediaWikiBot.queryAllusersByGroup(wikiGroupCode)) {
			userNames.add(user.getName());
		}

		if (wikiGroupCode.equals("bot")) {
			userNames.add("AlcoBot");
			userNames.add("Ashikbot");
			userNames.add("LankLinkBot");
		}

		Contributor teamContributor = new ContributorTeam(teamName, teamDescription);
		for (String userName : userNames) {
			List<Contributor> already = nameToContributors.get(userName);
			if (already == null) {
				already = new ArrayList<Contributor>(2);
				already.add(new ContributorUser(userName, skipIncrementPlace));
				nameToContributors.put(userName, already);
			}
			if (removeOtherTeams) {
				for (Iterator<Contributor> iterator = already.iterator(); iterator.hasNext();) {
					Contributor contributor = iterator.next();
					if (contributor instanceof ContributorTeam) {
						iterator.remove();
					}
				}
			}
			already.add(teamContributor);
		}
	}

	private Map<String, List<Contributor>> loadTeams() {
		final Map<String, List<Contributor>> nameToContributors = new HashMap<String, List<Contributor>>();

		loadTeam(nameToContributors, "bot", "Команда «Боты»", "Объединённая статистика по ботам", true, false);

		loadTeam(nameToContributors, "autoeditor", "Команда «Автопатрулируемые»",
				"Объединённая статистика по автопатрулируемым", false, true);
		loadTeam(nameToContributors, "editor", "Команда «Патрулирующие»", "Объединённая статистика по патрулирующим",
				false, true);
		loadTeam(nameToContributors, "closer", "Команда «ПИ»", "Объединённая статистика по подводящим итоги", false,
				true);
		loadTeam(nameToContributors, "sysop", "Команда «Администраторы»", "Объединённая статистика по администраторам",
				false, true);

		loadTeam(nameToContributors, "bureaucrat", "Команда «Бюрократы»", "Объединённая статистика по бюрократам",
				false, false);
		loadTeam(nameToContributors, "checkuser", "Команда «Чекюзеры»", "Объединённая статистика по чекюзерам", false,
				false);
		loadTeam(nameToContributors, "oversight", "Команда «Ревизоры»", "Объединённая статистика по ревизорам", false,
				false);

		return nameToContributors;
	}

	public void run(final Month month) throws Exception {

		final StatisticsKey statisticsKey = StatisticsKey.TOTAL;
		final TObjectLongHashMap<String> counters = loadCounters(month.getPagesCountsFile());

		List<String> byPopularity = new ArrayList<String>(counters.keySet());
		Collections.sort(byPopularity, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Long l1 = Long.valueOf(counters.get(o1));
				Long l2 = Long.valueOf(counters.get(o2));
				return l2.compareTo(l1);
			}
		});

		final Map<String, List<Contributor>> contributors = loadTeams();

		final TObjectDoubleHashMap<Contributor> byContributor = new TObjectDoubleHashMap<Contributor>(16, 1, 0);
		final Map<Contributor, TObjectDoubleMap<String>> byContributorArticleAuthorship = new HashMap<Contributor, TObjectDoubleMap<String>>();
		final Function<Contributor, Integer> userCountF = new Function<Contributor, Integer>() {
			@Override
			public Integer apply(Contributor сontributor) {
				return byContributorArticleAuthorship.get(сontributor).size();
			}
		};

		final Set<String> already = new HashSet<String>();
		final AtomicLong hitsAnalyzed = new AtomicLong(0);
		final AtomicInteger pagesAnalyzed = new AtomicInteger(0);
		final AtomicInteger pagesLogged = new AtomicInteger(0);

		final int maxQueue = Runtime.getRuntime().availableProcessors() * 2;
		final Semaphore semaphore = new Semaphore(maxQueue);

		for (final Revision latestRevisionId : mediaWikiBot.queryLatestRevisionsByPageTitles(byPopularity, true,
				WikiCache.FAST)) {
			semaphore.acquire(1);

			totalStatsExecutor.submit(new Runnable() {

				@Override
				public void run() {
					try {
						final ParsedPage page = (ParsedPage) latestRevisionId.getPage();
						final String pageTitle = page.getTitle();

						TextChunkList textChunks = revisionAuthorshipCalculator.getAuthorship(page, latestRevisionId,
								month.getEnd());
						if (textChunks == null) {
							System.out.println("Skip article: " + pageTitle);
							return;
						}

						long visits = 0;
						boolean articleProcessedBefore = false;
						synchronized (already) {
							if (already.add(pageTitle)) {
								visits += counters.get(pageTitle);
							} else {
								articleProcessedBefore = true;
							}
							if (page.getRedirectedFrom() != null) {
								for (String redirectedFrom : page.getRedirectedFrom()) {
									if (already.add(redirectedFrom)) {
										visits += counters.get(redirectedFrom);
									}
								}
							}
						}
						if (visits == 0) {
							return;
						}

						Map<String, Double> perArticle = textChunks.getAuthorshipProcents();
						for (String articleAuthor : perArticle.keySet()) {
							double percent = perArticle.get(articleAuthor);
							double value = percent * visits;

							if (renamedUsers.containsKey(articleAuthor)) {
								articleAuthor = renamedUsers.get(articleAuthor);
							}

							List<Contributor> countFor = getAsContributors(contributors, articleAuthor);
							for (Contributor contributor : countFor) {

								synchronized (byContributor) {
									byContributor.put(contributor, byContributor.get(contributor) + value);
								}

								if (!articleProcessedBefore) {
									TObjectDoubleMap<String> byArticle;
									synchronized (byContributorArticleAuthorship) {
										byArticle = byContributorArticleAuthorship.get(contributor);
										if (byArticle == null) {
											byArticle = new TObjectDoubleHashMap<String>(1, 1, 0);
											byContributorArticleAuthorship.put(contributor, byArticle);
										}
									}

									synchronized (byArticle) {
										final double newProcent = byArticle.get(pageTitle) + percent;
										if (newProcent > 1) {
											throw new AssertionError("Contribution of " + contributor + " in " + "'"
													+ pageTitle + "'" + " > 100%");
										}
										byArticle.put(pageTitle, newProcent);
									}
								}
							}
						}

						hitsAnalyzed.addAndGet(visits);
						if (!articleProcessedBefore) {
							pagesAnalyzed.incrementAndGet();
						}

						{
							final int analyzed = pagesAnalyzed.get();
							final int logged = pagesLogged.get();
							if (analyzed - logged > MIN_PAGES_DIFFERENCE) {
								if (pagesLogged.compareAndSet(logged, analyzed)) {
									/*
									 * if month already analyzed, we should not
									 * repeat writing of intermediate data
									 */
									if (!isMonthDone(statisticsKey, month, analyzed)) {
										write(statisticsKey, month, pagesAnalyzed.intValue(), hitsAnalyzed.longValue(),
												contributors, counters, byContributor, userCountF,
												byContributorArticleAuthorship, 5, 1000);
										setMonthDone(statisticsKey, month, analyzed);
									}
								}
							}
						}
					} catch (Exception exc) {
						log.error("Unable to calculate WikiStats data for " + latestRevisionId + ": " + exc, exc);
					} finally {
						semaphore.release(1);
					}
				}

			});
		}

		semaphore.acquire(maxQueue);

		write(statisticsKey, month, pagesAnalyzed.intValue(), hitsAnalyzed.longValue(), contributors, counters,
				byContributor, userCountF, byContributorArticleAuthorship, 5, 1000);

		setMonthDone(statisticsKey, month, pagesAnalyzed.get());
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	private void setMonthDone(StatisticsKey statisticsKey, Month month, int analyzed) {
		synchronized (doneMonth) {
			TObjectIntMap<Month> done = doneMonth.get(statisticsKey);
			if (done == null) {
				done = new TObjectIntHashMap<Month>(16, 1, 0);
				doneMonth.put(statisticsKey, done);
			}
			if (done.get(month) < analyzed) {
				done.put(month, analyzed);
			}
		}
	}

	public void setRevisionAuthorshipCalculator(RevisionAuthorshipCalculator revisionAuthorshipCalculator) {
		this.revisionAuthorshipCalculator = revisionAuthorshipCalculator;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}

	public void updateByTemplateIncluded(final StatisticsKey statisticsKey, final String template, final Month month)
			throws Exception {

		// TODO: redirects not counted

		final TObjectLongHashMap<String> counters = loadCounters(month.getPagesCountsFile());
		log.info("Loading user group: '" + "bot" + "'");
		TreeSet<String> userNames = new TreeSet<String>();
		for (User user : mediaWikiBot.queryAllusersByGroup("bot")) {
			userNames.add(user.getName());
		}

		final Map<String, List<Contributor>> contributors = loadTeams();

		TObjectDoubleHashMap<Contributor> byContributor = new TObjectDoubleHashMap<Contributor>(16, 1, 0);
		final Map<Contributor, TObjectDoubleMap<String>> byContributorByArticleProcents = new HashMap<Contributor, TObjectDoubleMap<String>>();

		final AtomicInteger pagesAnalyzed = new AtomicInteger(0);
		final AtomicLong hitsAnalyzed = new AtomicLong(0);

		for (final Revision latestRevisionContent : wikiCache.queryLatestContentByPageIds(mediaWikiBot
				.queryEmbeddedInPageIds(template, Namespaces.MAIN))) {

			final Page page = latestRevisionContent.getPage();
			final String pageTitle = page.getTitle();

			TextChunkList textChunks = revisionAuthorshipCalculator.getAuthorship(page, latestRevisionContent,
					month.getEnd());
			if (textChunks == null) {
				System.out.println("Skip article: " + page);
				continue;
			}

			long visits = counters.get(pageTitle);

			Map<String, Double> perArticle = textChunks.getAuthorshipProcents();
			for (String articleAuthor : perArticle.keySet()) {
				double percent = perArticle.get(articleAuthor);
				double value = percent * visits;

				if (renamedUsers.containsKey(articleAuthor)) {
					articleAuthor = renamedUsers.get(articleAuthor);
				}

				List<Contributor> countFor = getAsContributors(contributors, articleAuthor);

				for (Contributor contributor : countFor) {
					synchronized (byContributorByArticleProcents) {
						byContributor.put(contributor, byContributor.get(contributor) + value);
					}

					synchronized (byContributorByArticleProcents) {
						TObjectDoubleMap<String> contributorArticles = byContributorByArticleProcents.get(contributor);
						if (contributorArticles == null) {
							contributorArticles = new TObjectDoubleHashMap<String>(1, 1, 0);
							byContributorByArticleProcents.put(contributor, contributorArticles);
						}
						contributorArticles.put(pageTitle, contributorArticles.get(pageTitle) + percent);
					}
				}
			}

			pagesAnalyzed.incrementAndGet();
			hitsAnalyzed.addAndGet(visits);
		}

		write(statisticsKey, month, pagesAnalyzed.get(), hitsAnalyzed.get(), contributors, counters, byContributor,
				new Function<Contributor, Integer>() {
					@Override
					public Integer apply(Contributor contributor) {
						return byContributorByArticleProcents.get(contributor).size();
					}
				}, byContributorByArticleProcents, 10, 10000);

		setMonthDone(statisticsKey, month, pagesAnalyzed.get());
	}

	private void write(StatisticsKey statisticsKey, Month month, int pagesAnalyzed, long hitsAnalyzed,
			Map<String, List<Contributor>> teams, final TObjectLongHashMap<String> pageVisits,
			TObjectDoubleHashMap<Contributor> byContributor, Function<Contributor, Integer> byContributorArticlesCount,
			Map<Contributor, TObjectDoubleMap<String>> byContributorByArticleAuthorship, int maxArticlesToOutput,
			int maxPlacesToOutput) {

		final TObjectIntMap<Contributor> previousMonthStatistics;
		{
			final Month previousMonth = month.getPrevious();
			if (previousMonth == null) {
				previousMonthStatistics = null;
			} else {
				synchronized (places) {
					Map<Month, TObjectIntMap<Contributor>> monthPlaces = places.get(statisticsKey);
					if (monthPlaces == null) {
						previousMonthStatistics = null;
					} else {
						previousMonthStatistics = monthPlaces.get(previousMonth);
					}
				}
			}
		}

		List<Contributor> sorted = getKeysSortedByValueDesc(byContributor);

		StringBuffer buffer = new StringBuffer();
		buffer.append(statisticsKey.getStatisticsPageDescription(month, integerFormat.format(pagesAnalyzed),
				integerFormat.format(hitsAnalyzed)) + "\n\n");
		buffer.append("{| class=\"wikitable sortable\"\n");
		buffer.append("! colspan=2 | {{comment|Место|место по порядку в рейтинге}} !");
		buffer.append("! {{comment|Редактор|учитывается вклад всех редакторов, в том числе ботов}} !");
		if (byContributorArticlesCount != null) {
			buffer.append("! {{comment|Статей|количество статей, в редактировании которых участник принимал участие (из числа проанализированных)}} !");
		}
		if (byContributorByArticleAuthorship != null && maxArticlesToOutput > 0) {
			buffer.append("! {{comment|Статьи|список статей, количество посещений статьи, процент вклада участника и баллы за статью}} !");
		}
		buffer.append("! {{comment|Сумма баллов|сумма баллов за все статьи, в наполнении которых редактор принимал участие (из числа проанализированных)}}\n");
		buffer.append("|-\n");

		int placeCounter = 0;
		final TObjectIntMap<Contributor> placesMap = new TObjectIntHashMap<Contributor>();
		for (Contributor contributor : sorted) {

			int currentMonthPlace;
			if (contributor.isSkipIncrementPlace()) {
				currentMonthPlace = -1;
			} else {
				currentMonthPlace = ++placeCounter;
				placesMap.put(contributor, currentMonthPlace);
			}

			if (placeCounter >= maxPlacesToOutput) {
				continue;
			}

			if (contributor.isSkipIncrementPlace()) {
				buffer.append("|\n");
				buffer.append("|\n");
			} else {
				buffer.append("|align=right| " + currentMonthPlace + "\n");

				if (previousMonthStatistics == null) {
					buffer.append("||\n");
				} else if (!previousMonthStatistics.containsKey(contributor)) {
					buffer.append("|align=center|{{comment|{{up}}|new}}\n");
				} else {
					int prevMonthPlace = previousMonthStatistics.get(contributor);
					int change = prevMonthPlace - currentMonthPlace;

					if (change > 0) {
						buffer.append("|align=center|{{comment|{{up}}|+" + change + "}}\n");
					} else if (change < 0) {
						buffer.append("|align=center|{{comment|{{down}}|" + change + "}}\n");
					} else if (change == 0) {
						buffer.append("|align=center|{{нет изменений}}\n");
					}
				}
			}

			buffer.append("|" + contributor.toWiki() + "\n");

			if (byContributorArticlesCount != null) {
				buffer.append("|align=right|" + integerFormat.format(byContributorArticlesCount.apply(contributor))
						+ "\n");
			}

			if (byContributorByArticleAuthorship != null && maxArticlesToOutput > 0) {
				final TObjectDoubleMap<String> byArticleProcents = byContributorByArticleAuthorship.get(contributor);

				List<String> pageTitles = new ArrayList<String>(byArticleProcents.keySet());
				Collections.sort(pageTitles, new Comparator<String>() {
					@Override
					public int compare(String o1, String o2) {
						Double l1 = Double.valueOf(byArticleProcents.get(o1) * pageVisits.get(o1));
						Double l2 = Double.valueOf(byArticleProcents.get(o2) * pageVisits.get(o2));
						return l2.compareTo(l1);
					}
				});

				int toOutpuFull = Math.min(maxArticlesToOutput, pageTitles.size());
				int toOutputShort = pageTitles.size() - toOutpuFull;
				if (toOutputShort < 7) {
					toOutpuFull = pageTitles.size();
					toOutputShort = 0;
				}

				buffer.append("|{{Сокрытие|title=Список статей|hidden=1|content=\n");

				for (int i = 0; i < toOutpuFull; i++) {
					final String pageTitle = pageTitles.get(i);
					final double procents = byArticleProcents.get(pageTitle);
					final double articleProCent = 100 * procents;
					final long articleVisits = pageVisits.get(pageTitle);
					final long articlePoints = Math.round(procents * articleVisits);

					if (articlePoints == 0) {
						toOutpuFull = i;
						toOutputShort = pageTitles.size() - toOutpuFull;
						break;
					}

					buffer.append("* [[" + pageTitle + "]] (" + integerFormat.format(articleVisits) + ") — "
							+ decimalFormat.format(articleProCent) + "% (" + integerFormat.format(articlePoints)
							+ ")\n");
				}

				SummaryStatistics statArticlesCount = new SummaryStatistics();
				SummaryStatistics statArticlesAuthorship = new SummaryStatistics();
				SummaryStatistics statArticlesVisits = new SummaryStatistics();
				SummaryStatistics statArticlesPoints = new SummaryStatistics();

				if (toOutputShort > 0) {
					for (int i = toOutpuFull; i < pageTitles.size(); i++) {
						final String pageTitle = pageTitles.get(i);
						final double procents = byArticleProcents.get(pageTitle);
						final long articleVisits = pageVisits.get(pageTitle);
						final double articlePoints = procents * articleVisits;

						statArticlesCount.addValue(1);
						statArticlesAuthorship.addValue(procents);
						statArticlesVisits.addValue(articleVisits);
						statArticlesPoints.addValue(articlePoints);
					}

					final long sum = Math.round(statArticlesPoints.getSum());
					if (sum != 0) {
						if (toOutputShort < 5) {
							// due to "articlePoints == 0" break
							buffer.append("* За оставшиеся статьи (" + integerFormat.format(statArticlesCount.getSum())
									+ ") — " + integerFormat.format(statArticlesPoints.getSum()) + "\n");
						} else {
							buffer.append("* Оставшиеся статьи (" + integerFormat.format(statArticlesCount.getSum())
									+ "):\n");
							buffer.append("** Средний вклад: "
									+ decimalFormat.format(statArticlesAuthorship.getMean() * 100) + "% ± "
									+ decimalFormat.format(statArticlesAuthorship.getStandardDeviation() * 100) + "%\n");
							buffer.append("** Посещений: " + integerFormat.format(statArticlesVisits.getMean()) + " ± "
									+ integerFormat.format(statArticlesVisits.getStandardDeviation()) + "\n");
							buffer.append("** Баллов за статью: " + integerFormat.format(statArticlesPoints.getMean())
									+ " ± " + integerFormat.format(statArticlesPoints.getStandardDeviation()) + "\n");
							buffer.append("** Итого за оставшиеся: "
									+ integerFormat.format(statArticlesPoints.getSum()) + "\n");
						}
					}
				}

				buffer.append("}}\n");
			}

			double userTotalPoints = byContributor.get(contributor);
			buffer.append("|align=right|" + integerFormat.format(userTotalPoints) + "\n");
			buffer.append("|-\n");

			if (buffer.length() > 500000) {
				break;
			}
		}
		buffer.append("|}\n\n");

		buffer.append("[[Категория:Википедия:Рейтинги авторов]]\n");

		synchronized (places) {
			Map<Month, TObjectIntMap<Contributor>> byMonth = places.get(statisticsKey);
			if (byMonth == null) {
				byMonth = new HashMap<Month, TObjectIntMap<Contributor>>();
				places.put(statisticsKey, byMonth);
			}
			byMonth.put(month, placesMap);
		}

		mediaWikiBot.writeContent(
				"User:" + mediaWikiBot.getLogin() + "/" + statisticsKey.getStatisticsPageSuffix(month), null,
				buffer.toString(), null, "Обновление статистики (статей/хитов: " + integerFormat.format(pagesAnalyzed)
						+ " / " + integerFormat.format(hitsAnalyzed) + " )", false, false);
	}

}
