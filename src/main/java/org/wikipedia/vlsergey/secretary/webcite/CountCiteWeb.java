package org.wikipedia.vlsergey.secretary.webcite;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.mutable.MutableInt;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespaces;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;
import org.wikipedia.vlsergey.secretary.webcite.lists.SkipReason;

public class CountCiteWeb implements Runnable {

	private class Statistics {
		final Map<String, MutableInt> archived = new HashMap<String, MutableInt>();

		final Map<String, MutableInt> dead = new HashMap<String, MutableInt>();

		final String host;

		final MutableInt todo = new MutableInt(0);

		final MutableInt total = new MutableInt(0);

		Statistics(String host) {
			this.host = host;
		}
	}

	private static final Log log = LogFactory.getLog(CountCiteWeb.class);

	public static final int MIN_LINKS = 50;

	private Locale locale;

	private MediaWikiBot mediaWikiBot;

	private RefAwareParser refAwareParser;

	private WikiCache wikiCache;

	private WikiConstants wikiConstants;

	public Locale getLocale() {
		return locale;
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public RefAwareParser getRefAwareParser() {
		return refAwareParser;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	private void print(final Map<String, Statistics> byHost) {
		// update page
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("{| class=\"wikitable sortable\"\n");
		stringBuilder.append("|-\n");
		stringBuilder.append("! Host !! Skip reason !! Total !! Archived !! Dead !! To do\n");
		stringBuilder.append("|-\n");

		List<Statistics> statistics = new ArrayList<Statistics>(byHost.values());
		Collections.sort(statistics, new Comparator<Statistics>() {
			@Override
			public int compare(Statistics o1, Statistics o2) {
				return o2.total.compareTo(o1.total);
			}
		});

		for (Statistics stat : statistics) {
			if (stat.total.intValue() < MIN_LINKS) {
				break;
			}

			stringBuilder.append("| [[Special:Linksearch/" + stat.host + "|" + stat.host + "]]\n");

			// TODO: check utf-8 names
			URI testUri = URI.create("http://" + stat.host + "/");
			SkipReason skipReason = SkipReason.getSkipReason(testUri);
			if (skipReason == null) {
				stringBuilder.append("| &nbsp;\n");
			} else {
				stringBuilder.append("| " + skipReason.name() + "\n");
			}

			stringBuilder.append("| " + stat.total + "\n");

			printMap(stringBuilder, stat.archived);
			printMap(stringBuilder, stat.dead);

			if (skipReason == null) {
				stringBuilder.append("| " + stat.todo + "\n");
			} else {
				stringBuilder.append("| N/A\n");
			}

			stringBuilder.append("|-\n");
		}
		stringBuilder.append("|}");

		mediaWikiBot.writeContent("User:" + mediaWikiBot.getLogin() + "/CountCiteWeb", null, stringBuilder.toString(),
				null, "Update " + wikiConstants.template() + " statistics", true, false);
	}

	private int printMap(StringBuilder stringBuilder, Map<String, MutableInt> map) {
		int total = 0;
		for (MutableInt value : map.values()) {
			total += value.intValue();
		}
		stringBuilder.append("| " + total + "\n");

		List<String> keys = new ArrayList<String>(map.keySet());
		Collections.sort(keys);

		for (String key : keys) {
			stringBuilder.append("* " + key + " — " + map.get(key) + "\n");
		}

		return total;
	}

	@Override
	public void run() {

		final Map<String, Statistics> byHost = new HashMap<String, Statistics>();

		for (Revision revision : wikiCache.queryContentByPagesAndRevisions(mediaWikiBot
				.queryPagesWithRevisionByEmbeddedIn("Template:" + wikiConstants.template(),
						new int[] { Namespaces.MAIN }, new RevisionPropery[] { RevisionPropery.IDS }))) {

			final Page page = revision.getPage();
			final String title = page.getTitle();
			try {
				final String xmlContent = revision.getXml();
				{
					final String lowerCaseXml = xmlContent.toLowerCase();
					if (!lowerCaseXml.contains(wikiConstants.template())) {
						continue;
					}
				}

				ArticleFragment article = getRefAwareParser().parse(xmlContent);

				Map<String, List<Template>> allTemplates = article.getAllTemplates();

				if (allTemplates.containsKey(wikiConstants.template())) {
					for (Template template : allTemplates.get(wikiConstants.template())) {
						updateStatistics(byHost, template);
					}
				}

			} catch (Exception exc) {
				log.warn(title + ": " + exc.getMessage());
			}
		}

		print(byHost);
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
		this.wikiConstants = WikiConstants.get(locale);
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

	private void updateStatistics(Map<String, Statistics> byHost, Template template) {
		final Content urlValue = wikiConstants.url(template);
		final Content archiveUrlValue = wikiConstants.archiveUrl(template);
		final Content deadLink = wikiConstants.deadlink(template);

		if (urlValue == null) {
			return;
		}

		String urlString = StringUtils.trimToEmpty(urlValue.toWiki(true));
		String archiveUrlString = archiveUrlValue != null ? StringUtils.trimToEmpty(archiveUrlValue.toWiki(true))
				: null;
		String deadLinkString = deadLink != null ? StringUtils.trimToEmpty(deadLink.toWiki(true)).toLowerCase(
				getLocale()) : null;

		try {
			URI uri = new URI(urlString);
			String host = uri.getHost();

			Statistics statistics = byHost.get(host);
			if (statistics == null) {
				statistics = new Statistics(host);
				byHost.put(host, statistics);
			}

			statistics.total.increment();

			if (StringUtils.isNotBlank(deadLinkString)) {
				MutableInt deadCounter = statistics.dead.get(deadLinkString);
				if (deadCounter == null) {
					deadCounter = new MutableInt();
					statistics.dead.put(deadLinkString, deadCounter);
				}
				deadCounter.increment();
			}

			if (StringUtils.isNotBlank(archiveUrlString)) {
				String archiveHost;
				try {
					archiveHost = URI.create(archiveUrlString).getHost();
				} catch (Exception exc) {
					archiveHost = "error";
				}

				if (StringUtils.isNotBlank(archiveHost)) {
					MutableInt archiveCounter = statistics.archived.get(archiveHost);
					if (archiveCounter == null) {
						archiveCounter = new MutableInt();
						statistics.archived.put(archiveHost, archiveCounter);
					}
					archiveCounter.increment();
				}
			}

			if (StringUtils.isBlank(deadLinkString) && StringUtils.isBlank(archiveUrlString)) {
				statistics.todo.increment();
			}

		} catch (URISyntaxException exc) {
			log.debug("Incorrect URL: " + urlString + ": " + exc);
		} catch (Exception exc) {
			log.debug("Unable to process cite web template with URL " + urlString + ": " + exc, exc);
		}

	}
}
