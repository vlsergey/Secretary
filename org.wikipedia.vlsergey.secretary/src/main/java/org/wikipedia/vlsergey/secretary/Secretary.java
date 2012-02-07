package org.wikipedia.vlsergey.secretary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wikipedia.vlsergey.secretary.functions.IteratorUtils;
import org.wikipedia.vlsergey.secretary.http.ExternalIpChecker;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMember;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMemberType;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespaces;
import org.wikipedia.vlsergey.secretary.webcite.QueuedLinkProcessor;
import org.wikipedia.vlsergey.secretary.webcite.QueuedPageDao;
import org.wikipedia.vlsergey.secretary.webcite.QueuedPageProcessor;
import org.wikipedia.vlsergey.secretary.webcite.WebCiteArchiver;

public class Secretary {

	private static final Log logger = LogFactory.getLog(Secretary.class);

	private static final String login;

	private static final String password;

	private static final String site;

	static {
		login = System.getProperty("org.wikipedia.vlsergey.bot.login");
		password = System.getProperty("org.wikipedia.vlsergey.bot.password");
		site = System.getProperty("org.wikipedia.vlsergey.bot.site");

		if (login == null)
			System.err.println("Login (org.wikipedia.vlsergey.bot.login)"
					+ " not specified");
		if (password == null)
			System.err.println("Password (org.wikipedia.vlsergey.bot.password)"
					+ " not specified");
		if (site == null)
			System.err.println("Site (org.wikipedia.vlsergey.bot.site)"
					+ " not specified");
	}

	public static void main(String[] args) throws Exception {
		ApplicationContext appContext = new ClassPathXmlApplicationContext(
				"application-context.xml");

		ExternalIpChecker ExternalIpChecker = appContext
				.getBean(ExternalIpChecker.class);
		ExternalIpChecker.assertIpAddressesAreDifferent();

		MediaWikiBot mediaWikiBot = appContext.getBean(MediaWikiBot.class);
		mediaWikiBot.httpLogin(login, password);

		// FilesCategoryHelper filesCategoryHelper = appContext
		// .getBean(FilesCategoryHelper.class);
		// filesCategoryHelper.run();

		// WebCiteChecker webCiteChecker = appContext
		// .getBean(WebCiteChecker.class);
		// webCiteChecker.run();

		WebCiteArchiver webCiteArchiver = appContext
				.getBean(WebCiteArchiver.class);
		webCiteArchiver.updateIgnoringList();

		QueuedLinkProcessor queuedLinkProcessor = appContext
				.getBean(QueuedLinkProcessor.class);
		queuedLinkProcessor.start();

		QueuedPageDao queuedPageDao = appContext.getBean(QueuedPageDao.class);
		QueuedPageProcessor queuedPageProcessor = appContext
				.getBean(QueuedPageProcessor.class);

		queuedPageProcessor.clearQueue();
		// queuedLinkProcessor.clearQueue();

		for (Long pageId : IteratorUtils.map(mediaWikiBot.queryCategoryMembers(
				"Категория:Википедия:Пресса о Википедии:Архив",
				CategoryMemberType.PAGE, Namespaces.PROJECT),
				CategoryMember.pageIdF)) {
			queuedPageDao.addPageToQueue(pageId, 5000, 0);
		}

		for (Long pageId : mediaWikiBot
				.queryEmbeddedInPageIds("Шаблон:Избранная статья")) {
			queuedPageDao.addPageToQueue(pageId, 1000, pageId.longValue());
		}
		for (Long pageId : mediaWikiBot
				.queryEmbeddedInPageIds("Шаблон:Хорошая статья")) {
			queuedPageDao.addPageToQueue(pageId, 500, pageId.longValue());
		}
		for (Long pageId : mediaWikiBot
				.queryEmbeddedInPageIds("Шаблон:Cite web")) {
			queuedPageDao.addPageToQueue(pageId, 0, pageId.longValue());
		}

		// for (String articleTitle : new String[] {
		// "Президентские выборы в Белоруссии (2006)", "" }) {
		// queuedPageDao.addPageToQueue(
		// mediaWikiBot
		// .queryRevisionLatest(articleTitle,
		// RevisionPropery.IDS).getPage().getId(),
		// 5000, 0);
		// }

		queuedPageProcessor.run();

		Thread.sleep(72l * 60l * 60l * 1000l);
	}
}
