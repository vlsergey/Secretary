package org.wikipedia.vlsergey.secretary.patrollists;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.FilterRedirects;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;

public class BuildUnreviewedLists implements Runnable {

	private static final Log logger = LogFactory.getLog(BuildUnreviewedLists.class);

	private final Map<Long, String> allNamespacesNoRedirects = new HashMap<Long, String>();

	private final Map<Long, String> allNamespacesRedirects = new HashMap<Long, String>();

	private MediaWikiBot mediaWikiBot;

	private final Set<Long> redirects = new HashSet<Long>();

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	@Override
	public void run() {
		try {

			runImpl1(Namespace.MAIN, "Articles", "статьи", false);
			runImpl1(Namespace.FILE, "Files", "файлы", true);
			runImpl1(Namespace.TEMPLATE, "Templates", "шаблоны", true);
			runImpl1(Namespace.CATEGORY, "Categories", "категории", true);
			runImpl1(Namespace.RU_WIKI_PORTAL, "Portals", "порталы", true);

			updateStatistics(
					"Непроверенные (неотпатрулированные) страницы (без перенаправлений), отсортированные по времени создания",
					"Участник:Vlsergey/UnreviewedNonRedirects", allNamespacesNoRedirects);

			updateStatistics(
					"Непроверенные (неотпатрулированные) перенаправления, отсортированные по времени создания",
					"Участник:Vlsergey/UnreviewedRedirects", allNamespacesRedirects);

			Map<Long, String> total = new HashMap<Long, String>();
			total.putAll(allNamespacesNoRedirects);
			total.putAll(allNamespacesRedirects);

			updateStatistics(
					"Непроверенные (неотпатрулированные) страницы (включая перенаправления), отсортированные по времени создания",
					"Участник:Vlsergey/UnreviewedPages", total);

		} catch (Exception exc) {
			logger.error("" + exc, exc);
		}
	}

	private void runImpl1(Namespace namespace, String namespaceTitle, String namespaceDesc, boolean ignoreRedirects)
			throws Exception {

		if (ignoreRedirects) {
			Map<Long, String> all = startImpl2(namespace, FilterRedirects.ALL, "Непроверенные (неотпатрулированные) "
					+ namespaceDesc + ", отсортированные по времени создания", "Участник:Vlsergey/UnreviewedPages/"
					+ namespaceTitle);

			allNamespacesNoRedirects.putAll(all);
			return;
		}

		Map<Long, String> noRedirects = startImpl2(namespace, FilterRedirects.NONREDIRECTS,
				"Непроверенные (неотпатрулированные) " + namespaceDesc
						+ " (без перенаправлений), отсортированные по времени создания",
				"Участник:Vlsergey/UnreviewedNonRedirects/" + namespaceTitle);

		Map<Long, String> redirects = startImpl2(namespace, FilterRedirects.REDIRECTS,
				"Непроверенные (неотпатрулированные) перенаправления на " + namespaceDesc
						+ ", отсортированные по времени создания", "Участник:Vlsergey/UnreviewedRedirects/"
						+ namespaceTitle);

		Map<Long, String> total = new HashMap<Long, String>();
		total.putAll(noRedirects);
		total.putAll(redirects);

		updateStatistics("Непроверенные (неотпатрулированные) " + namespaceDesc
				+ " (включая перенаправления), отсортированные по времени создания",
				"Участник:Vlsergey/UnreviewedPages/" + namespaceTitle, total);

		allNamespacesNoRedirects.putAll(noRedirects);
		allNamespacesRedirects.putAll(redirects);
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	private Map<Long, String> startImpl2(Namespace namespace, FilterRedirects filterRedirects, String description,
			String statisticsPage) throws Exception {
		Map<Long, String> pages = new HashMap<Long, String>();

		if (filterRedirects == FilterRedirects.ALL) {

			for (Page page : mediaWikiBot
					.queryUnreviewedPages(new Namespace[] { namespace }, FilterRedirects.REDIRECTS)) {
				pages.put(page.getId(), page.getTitle());
			}

			redirects.addAll(pages.keySet());

			for (Page page : mediaWikiBot.queryUnreviewedPages(new Namespace[] { namespace },
					FilterRedirects.NONREDIRECTS)) {
				pages.put(page.getId(), page.getTitle());
			}

		} else {

			for (Page page : mediaWikiBot.queryUnreviewedPages(new Namespace[] { namespace }, filterRedirects)) {
				pages.put(page.getId(), page.getTitle());
			}

			if (filterRedirects == FilterRedirects.REDIRECTS)
				redirects.addAll(pages.keySet());
		}

		updateStatistics(description, statisticsPage, pages);

		return pages;
	}

	private void updateStatistics(String description, String statisticsPage, Map<Long, String> pages) throws Exception {
		List<Long> ids = new ArrayList<Long>(pages.keySet());
		Collections.sort(ids);

		String article = description + "\n\n";

		int counter = 0;
		for (Long id : ids) {
			final String pageName = pages.get(id);

			if (redirects.contains(id)) {
				article += "# <small>(" + id + ")</small> [{{fullurl:" + pageName + "|redirect=no}} " + pageName + "] "
						+ "\n";
			} else {
				article += "# <small>(" + id + ")</small> [[:" + pageName + "]]" + "\n";
			}

			counter++;
			if (counter >= 1000)
				break;
		}

		mediaWikiBot.writeContent(statisticsPage, null, article, null, "Update unreviewed pages list", false, false);
	}
}
