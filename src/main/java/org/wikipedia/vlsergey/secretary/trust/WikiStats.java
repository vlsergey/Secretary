package org.wikipedia.vlsergey.secretary.trust;

import gnu.trove.map.hash.TObjectDoubleHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespaces;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class WikiStats {

	private static final DecimalFormat decimalFormat = new DecimalFormat("0.00");

	private static final int MAX_USERS = 500;

	private static String dump(final TObjectDoubleHashMap<String> byUser) {

		List<String> userNames = getKeysSortedByValueDesc(byUser);

		int count = 0;
		StringBuilder stringBuilder = new StringBuilder();
		for (String userName : userNames) {
			double value = byUser.get(userName);
			count++;
			final String strValue = decimalFormat.format(value);
			stringBuilder.append("# [[User:" + userName + "|" + userName + "]] — " + strValue + " ;\n");

			if (count == MAX_USERS) {
				break;
			}
		}

		return stringBuilder.toString();

	}

	private static List<String> getKeysSortedByValueDesc(final TObjectDoubleHashMap<String> byUser) {
		List<String> userNames = new ArrayList<String>(byUser.keySet());
		Collections.sort(userNames, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Double l1 = byUser.get(o1);
				Double l2 = byUser.get(o2);
				return l2.compareTo(l1);
			}
		});
		return userNames;
	}

	private static Date lastAllowedEditTimestamp() throws ParseException {
		return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ").parse("2013-07-01T00:00:00.000+0000");
	}

	private Locale locale;

	private MediaWikiBot mediaWikiBot;

	private RevisionAuthorshipCalculator revisionAuthorshipCalculator;

	public Locale getLocale() {
		return locale;
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public RevisionAuthorshipCalculator getRevisionAuthorshipCalculator() {
		return revisionAuthorshipCalculator;
	}

	private TObjectLongHashMap<String> loadCounters() throws UnsupportedEncodingException, FileNotFoundException,
			IOException {
		final TObjectLongHashMap<String> counters = new TObjectLongHashMap<String>(16, 1, 0);

		{
			LineNumberReader reader = new LineNumberReader(new InputStreamReader(new FileInputStream(
					"stats/stats-2013-06-01.txt"), "utf-8"));
			try {
				String line;
				while ((line = reader.readLine()) != null) {
					if (StringUtils.isBlank(line)) {
						continue;
					}
					String[] strings = StringUtils.split(line, "\t");
					counters.put(strings[0], Long.valueOf(strings[1]));
				}
			} finally {
				reader.close();
			}
		}
		return counters;
	}

	public void run() throws Exception {

		final TObjectLongHashMap<String> counters = loadCounters();

		List<String> byPopularity = new ArrayList<String>(counters.keySet());
		Collections.sort(byPopularity, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Long l1 = Long.valueOf(counters.get(o1));
				Long l2 = Long.valueOf(counters.get(o2));
				return l2.compareTo(l1);
			}
		});

		TObjectDoubleHashMap<String> byUser = new TObjectDoubleHashMap<String>(16, 1, 0);

		long prevCounter = 0;
		long counter = 0;
		for (String articleName : byPopularity) {

			List<TextChunk> textChunks = revisionAuthorshipCalculator.getAuthorship(articleName,
					lastAllowedEditTimestamp());
			if (textChunks == null) {
				System.out.println("Skip article: " + articleName);
				continue;
			}

			long visits = counters.get(articleName);

			Map<String, Double> perArticle = RevisionAuthorshipCalculator.getProcents(textChunks);
			for (String articleAuthor : perArticle.keySet()) {
				double percent = perArticle.get(articleAuthor);
				double value = percent * visits;

				byUser.put(articleAuthor, byUser.get(articleAuthor) + value);
			}

			if (counter - prevCounter > 1000000) {
				mediaWikiBot.writeContent("User:" + mediaWikiBot.getLogin() + "/WikiRaiting/2013-07", null,
						"Рейтинг авторов по посещаемости их статей, основано на анализе " + counter + " хитов.\n\n"
								+ dump(byUser), null, "Update stats on " + counter + " hits", true, false);
				prevCounter = counter;
			}
		}

		mediaWikiBot.writeContent("User:" + mediaWikiBot.getLogin() + "/WikiRaiting/2013-07", null,
				"Рейтинг авторов по посещаемости их статей, основано на анализе " + counter
						+ " хитов (всё, что доступно боту).\n\n" + dump(byUser), null, "Update stats (final) ", true,
				false);
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setRevisionAuthorshipCalculator(RevisionAuthorshipCalculator revisionAuthorshipCalculator) {
		this.revisionAuthorshipCalculator = revisionAuthorshipCalculator;
	}

	public void updateByTemplateIncluded(final String statPageTitle, final String description, final String template,
			final boolean updateAfterEach) throws Exception {

		final TObjectLongHashMap<String> counters = loadCounters();

		TObjectDoubleHashMap<String> byUser = new TObjectDoubleHashMap<String>(16, 1, 0);
		final Map<String, SortedMap<String, Double>> perUserProcents = new HashMap<String, SortedMap<String, Double>>();

		for (final String pageTitle : mediaWikiBot.queryEmbeddedInPageTitles(template, Namespaces.MAIN)) {
			List<TextChunk> textChunks = revisionAuthorshipCalculator.getAuthorship(pageTitle,
					lastAllowedEditTimestamp());
			if (textChunks == null) {
				System.out.println("Skip article: " + pageTitle);
				continue;
			}

			long visits = counters.get(pageTitle);

			Map<String, Double> perArticle = RevisionAuthorshipCalculator.getProcents(textChunks);
			for (String articleAuthor : perArticle.keySet()) {
				double percent = perArticle.get(articleAuthor);
				double value = percent * visits;

				byUser.put(articleAuthor, byUser.get(articleAuthor) + value);

				SortedMap<String, Double> userArticles = perUserProcents.get(articleAuthor);
				if (userArticles == null) {
					userArticles = new TreeMap<String, Double>();
					perUserProcents.put(articleAuthor, userArticles);
				}
				userArticles.put(pageTitle, Double.valueOf(percent));
			}

			if (updateAfterEach) {
				try {
					write(statPageTitle, description, counters, byUser, perUserProcents);
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		}

		write(statPageTitle, description, counters, byUser, perUserProcents);
	}

	private void write(String statPageTitle, String description, TObjectLongHashMap<String> counters,
			TObjectDoubleHashMap<String> byUser, Map<String, SortedMap<String, Double>> perUserProcents) {

		List<String> sorted = getKeysSortedByValueDesc(byUser);

		StringBuffer buffer = new StringBuffer();
		buffer.append(description + "\n\n");
		buffer.append("{| class=\"wikitable sortable\"\n");
		buffer.append("! {{comment|Редактор|учитывается вклад всех редакторов, в том числе ботов}} !"
				+ "! {{comment|Статей|количество статей, в редактировании которых участник принимал участие (из числа проанализированных)}} !"
				+ "! {{comment|Статьи|список статей, количество посещений статьи, процент вклада участника и баллы за статью}} !"
				+ "! {{comment|Сумма баллов|сумма баллов за все статьи, в наполнении которых редактор принимал участие (из числа проанализированных)}}\n");
		buffer.append("|-\n");
		for (String userName : sorted) {
			double userTotalPoints = byUser.get(userName);
			SortedMap<String, Double> userArticles = perUserProcents.get(userName);

			buffer.append("| [[User:" + userName + "|" + userName + "]]\n");
			buffer.append("| align=right | " + userArticles.size() + "\n");
			buffer.append("| {{Сокрытие|title=Список статей|hidden=1|content=\n");
			for (Map.Entry<String, Double> entry : userArticles.entrySet()) {

				String articleTitle = entry.getKey();
				double articleProCent = 100 * entry.getValue().doubleValue();
				final long articleVisits = counters.get(articleTitle);
				double articlePoints = entry.getValue().doubleValue() * articleVisits;

				buffer.append("* [[" + articleTitle + "]] (" + articleVisits + ") — "
						+ decimalFormat.format(articleProCent) + "% (" + decimalFormat.format(articlePoints) + ")\n");
			}
			buffer.append("}}\n");
			buffer.append("| align=right | " + decimalFormat.format(userTotalPoints) + "\n");
			buffer.append("|-\n");
		}
		buffer.append("|}\n");

		mediaWikiBot.writeContent("User:" + mediaWikiBot.getLogin() + "/" + statPageTitle, null, buffer.toString(),
				null, "Обновление статистики", false, false);
	}
}
