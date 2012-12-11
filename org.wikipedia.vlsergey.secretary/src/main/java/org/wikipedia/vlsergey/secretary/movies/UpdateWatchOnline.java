package org.wikipedia.vlsergey.secretary.movies;

import java.io.StringReader;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Comment;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.Text;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

import au.com.bytecode.opencsv.CSVReader;

public class UpdateWatchOnline implements Runnable {

	private MediaWikiBot mediaWikiBot;

	@Autowired
	private TaskScheduler taskScheduler;

	private WikiCache wikiCache;

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	@PostConstruct
	public void init() throws Exception {
		taskScheduler.scheduleWithFixedDelay(this, DateUtils.MILLIS_PER_DAY);
	}

	private Map<String, String> loadIvi() throws Exception {
		Map<String, String> result = new HashMap<String, String>();
		String data = IoUtils.readToString(UpdateWatchOnline.class.getResourceAsStream("ivi.csv"), "utf-8");

		CSVReader csvReader = new CSVReader(new StringReader(data));
		for (String[] line : csvReader.readAll()) {
			if (line.length < 6)
				continue;

			final String externalUriStr = StringUtils.trim(line[4]);
			if (!externalUriStr.startsWith("http://www.ivi.ru/"))
				continue;

			final URI external = new URI(externalUriStr);
			final String wikiUriStr = StringUtils.trim(line[6]);

			if (!wikiUriStr.startsWith("http://ru.wikipedia.org/wiki/"))
				continue;

			URI wiki = new URI(wikiUriStr);
			String articleName = wiki.getPath().substring(6).replace("_", " ");

			result.put(articleName, external.toString());
		}
		return result;
	}

	private Map<String, String> loadMediawikiCommons() throws Exception {
		Map<String, String> result = new HashMap<String, String>();
		result.put("Разбудите Леночку", "Файл:Razbudite_Lenochku_(1934).ogv");
		result.put("Чапаев (фильм)", "Файл:Чапаев (фильм).ogv");
		return result;
	}

	private Map<String, String> loadMolodejjTv() throws Exception {
		Map<String, String> result = new HashMap<String, String>();
		result.put("Ты и я (фильм, 2011)", "http://www.molodejj.tv/films/comedy/films_komedii_ti_i_ya/");
		return result;
	}

	private Map<String, String> loadPulter() throws Exception {
		Map<String, String> result = new HashMap<String, String>();
		result.put("13 стульев", "http://pulter.ru/cinema/13_stulev/s01");
		return result;
	}

	private Map<String, String> loadTvZavr() throws Exception {
		Map<String, String> result = new HashMap<String, String>();
		String data = IoUtils.readToString(UpdateWatchOnline.class.getResourceAsStream("tvzavr.csv"), "utf-8");

		CSVReader csvReader = new CSVReader(new StringReader(data));
		for (String[] line : csvReader.readAll()) {
			if (line.length != 5)
				continue;

			final String externalUriStr = StringUtils.trim(line[3]);
			if (!externalUriStr.startsWith("http://www.tvzavr.ru/"))
				continue;

			final URI external = new URI(externalUriStr);
			final String wikiUriStr = StringUtils.trim(line[4]);

			if (!wikiUriStr.startsWith("http://ru.wikipedia.org/wiki/"))
				continue;

			URI wiki = new URI(wikiUriStr);
			String articleName = wiki.getPath().substring(6).replace("_", " ");

			result.put(articleName, external.toString());
		}
		return result;
	}

	@Override
	public void run() {
		try {
			start();
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}

	private void start() throws Exception {

		Map<String, Map<String, String>> known = new HashMap<String, Map<String, String>>();

		known.put("commons", loadMediawikiCommons());
		known.put("ivi.ru", loadIvi());
		known.put("molodejj.tv", loadMolodejjTv());
		known.put("pulter.ru", loadPulter());
		known.put("tvzavr.ru", loadTvZavr());

		Set<String> knownMovies = new TreeSet<String>();
		for (Map<String, ?> values : known.values()) {
			knownMovies.addAll(values.keySet());
		}

		for (String knownMovie : knownMovies) {
			Template viewOnline = new Template(new Text("Просмотр онлайн"));

			for (Map.Entry<String, Map<String, String>> provider : known.entrySet()) {
				String providerParameterName = provider.getKey();

				if (provider.getValue().containsKey(knownMovie)) {
					String external = provider.getValue().get(knownMovie);

					viewOnline.setParameterValue(providerParameterName, new Text(external));
				}
			}
			viewOnline.format(true, false);

			ArticleFragment template = new ArticleFragment(
					Arrays.asList(
							new Comment(
									"Данный шаблон обновляется ботом. \n"
											+ "Пожалуйста, не исправляйте его руками, так как все изменения будут затёрты автоматически в течении 24-х часов."),
							viewOnline));

			final String pageTitle = "Шаблон:Просмотр онлайн/" + knownMovie;
			final String newText = template.toWiki(false);
			final String oldText = wikiCache.queryLatestRevisionContent(pageTitle);

			if (!StringUtils.equals(oldText, newText)) {
				getMediaWikiBot().writeContent(pageTitle, null, newText, null,
						"Обновление списка сайтов просмотра фильма online", false, false);
			}
		}
	}
}
