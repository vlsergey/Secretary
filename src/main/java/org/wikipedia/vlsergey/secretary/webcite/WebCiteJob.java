package org.wikipedia.vlsergey.secretary.webcite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.functions.IteratorUtils;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMember;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMemberType;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespaces;

public class WebCiteJob implements Runnable {

	private Locale locale;

	private MediaWikiBot mediaWikiBot;

	@Autowired
	private QueuedLinkProcessor queuedLinkProcessor;

	@Autowired
	private QueuedPageDao queuedPageDao;

	@Autowired
	private QueuedPageProcessor queuedPageProcessor;

	// @Autowired
	// private WebCiteErrorCleanup webCiteErrorCleanup;

	public Locale getLocale() {
		return locale;
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	@PostConstruct
	public void init() throws Exception {
		updateIgnoringList();
		queuedLinkProcessor.start();
	}

	@Override
	public void run() {

		// webCiteErrorCleanup.errorCleanup("http://news.euro-coins.info/");
		queuedPageProcessor.clearQueue();
		queuedLinkProcessor.clearQueue();

		if ("ru".equals(getLocale().getLanguage())) {
			for (Long pageId : IteratorUtils.map(mediaWikiBot.queryCategoryMembers(
					"Категория:Википедия:Пресса о Википедии:Архив", CategoryMemberType.PAGE, Namespaces.PROJECT),
					CategoryMember.pageIdF)) {
				queuedPageDao.addPageToQueue(getLocale(), pageId, 5000, 0);
			}

			for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Избранная статья", Namespaces.MAIN)) {
				queuedPageDao.addPageToQueue(getLocale(), pageId, 1000, pageId.longValue());
			}
			for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Хорошая статья", Namespaces.MAIN)) {
				queuedPageDao.addPageToQueue(getLocale(), pageId, 500, pageId.longValue());
			}
			for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Избранный список или портал",
					Namespaces.MAIN)) {
				queuedPageDao.addPageToQueue(getLocale(), pageId, 500, pageId.longValue());
			}
		}

		if ("uk".equals(getLocale().getLanguage())) {
			for (Long pageId : IteratorUtils.map(mediaWikiBot.queryCategoryMembers(
					"Категорія:Вікіпедія:Публікації про Вікіпедію", CategoryMemberType.PAGE, Namespaces.PROJECT),
					CategoryMember.pageIdF)) {
				queuedPageDao.addPageToQueue(getLocale(), pageId, 5000, 0);
			}

			for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Медаль", Namespaces.MAIN)) {
				queuedPageDao.addPageToQueue(getLocale(), pageId, 1000, pageId.longValue());
			}
			for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Добра_стаття", Namespaces.MAIN)) {
				queuedPageDao.addPageToQueue(getLocale(), pageId, 500, pageId.longValue());
			}
			for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Вибраний_список", Namespaces.MAIN)) {
				queuedPageDao.addPageToQueue(getLocale(), pageId, 500, pageId.longValue());
			}
		}

		for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Template:Cite web", Namespaces.MAIN)) {
			queuedPageDao.addPageToQueue(getLocale(), pageId, 0, pageId.longValue());
		}

		queuedPageProcessor.run();
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void updateIgnoringList() throws Exception {
		updateIgnoringList(WebCiteArchiver.SKIP_ERRORS, "User:" + getMediaWikiBot().getLogin() + "/IgnoreErrors");
		updateIgnoringList(WebCiteArchiver.SKIP_NO_CACHE, "User:" + getMediaWikiBot().getLogin() + "/IgnoreNoCache");
		updateIgnoringList(WebCiteArchiver.SKIP_ARCHIVES, "User:" + getMediaWikiBot().getLogin() + "/IgnoreSence");
		updateIgnoringList(WebCiteArchiver.SKIP_TECH_LIMITS, "User:" + getMediaWikiBot().getLogin()
				+ "/IgnoreTechLimits");
	}

	private void updateIgnoringList(Set<String> hostsToIgnore, String pageName) throws Exception {
		StringBuffer stringBuffer = new StringBuffer();

		List<String> hosts = new ArrayList<String>(hostsToIgnore);
		Collections.sort(hosts, new Comparator<String>() {

			final Map<String, String> cache = new HashMap<String, String>();

			@Override
			public int compare(String o1, String o2) {

				String s1 = inverse(o1);
				String s2 = inverse(o2);

				return s1.compareToIgnoreCase(s2);
			}

			private String inverse(String direct) {
				String result = cache.get(direct);
				if (result != null)
					return result;

				String[] splitted = StringUtils.split(direct, ".");
				Collections.reverse(Arrays.asList(splitted));
				result = StringUtils.join(splitted, ".");
				cache.put(direct, result);
				return result;
			}
		});

		for (String hostName : hosts) {
			stringBuffer.append("* " + hostName + "\n");
		}

		mediaWikiBot.writeContent(pageName, null, stringBuffer.toString(), null, "Update ignoring sites list", true,
				false);
	}

}
