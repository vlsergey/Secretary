package org.wikipedia.vlsergey.secretary;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.webcite.QueuedLinkProcessor;
import org.wikipedia.vlsergey.secretary.webcite.QueuedPageDao;
import org.wikipedia.vlsergey.secretary.webcite.QueuedPageProcessor;

public class Secretary {

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

		MediaWikiBot mediaWikiBot = appContext.getBean(MediaWikiBot.class);
		mediaWikiBot.httpLogin(login, password);

		QueuedLinkProcessor queuedLinkProcessor = appContext
				.getBean(QueuedLinkProcessor.class);
		queuedLinkProcessor.start();

		QueuedPageDao queuedPageDao = appContext.getBean(QueuedPageDao.class);
		for (Long pageId : mediaWikiBot
				.queryEmbeddedInPageIds("Шаблон:Избранная статья")) {
			queuedPageDao.addPageToQueue(pageId, pageId.longValue());
		}

		// for (String articleTitle : new String[] { "Маскаро, Хуан",
		// "Гигантское магнетосопротивление", "Мор, Георг",
		// "Шривастава, Чандрика Прасад", "Тагор, Рабиндранат",
		// "Десятинная церковь", "Варанаси", "Айодхья" }) {
		// queuedPageDao.addPageToQueue(
		// mediaWikiBot
		// .queryRevisionLatest(articleTitle,
		// RevisionPropery.IDS).getPage().getId(), 0);
		// }

		QueuedPageProcessor queuedPageProcessor = appContext
				.getBean(QueuedPageProcessor.class);
		queuedPageProcessor.run();
	}
}
