package org.wikipedia.vlsergey.secretary;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wikipedia.vlsergey.secretary.http.ExternalIpChecker;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.webcite.WebCiteChecker;

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

		ExternalIpChecker ExternalIpChecker = appContext
				.getBean(ExternalIpChecker.class);
		ExternalIpChecker.assertIpAddressesAreDifferent();

		MediaWikiBot mediaWikiBot = appContext.getBean(MediaWikiBot.class);
		mediaWikiBot.httpLogin(login, password);

		WebCiteChecker webCiteChecker = appContext
				.getBean(WebCiteChecker.class);
		webCiteChecker.run();

		// WebCiteArchiver webCiteArchiver = appContext
		// .getBean(WebCiteArchiver.class);
		// webCiteArchiver.updateIgnoringList();

		// QueuedLinkProcessor queuedLinkProcessor = appContext
		// .getBean(QueuedLinkProcessor.class);
		// queuedLinkProcessor.start();

		// QueuedPageDao queuedPageDao =
		// appContext.getBean(QueuedPageDao.class);
		// for (Long pageId : mediaWikiBot
		// .queryEmbeddedInPageIds("Шаблон:Избранная статья")) {
		// queuedPageDao.addPageToQueue(pageId, 1000, pageId.longValue());
		// }
		// for (Long pageId : mediaWikiBot
		// .queryEmbeddedInPageIds("Шаблон:Хорошая статья")) {
		// queuedPageDao.addPageToQueue(pageId, 500, pageId.longValue());
		// }
		//
		// for (String articleTitle : new String[] {
		// "Википедия:Пресса о Википедии" }) {
		// queuedPageDao.addPageToQueue(
		// mediaWikiBot
		// .queryRevisionLatest(articleTitle,
		// RevisionPropery.IDS).getPage().getId(),
		// 5000, 0);
		// }

		// for (String articleTitle : new String[] { "Unlimited Detail",
		// "F.E.A.R. 2: Reborn", "CellFactor: Combat Training",
		// "CellFactor: Revolution", "Ageia", "NovodeX", "Meqon",
		// "Reality Engine", "Прямая кинематика", "Инверсная кинематика",
		// "Bullet Physics Library", "Open Physics Initiative",
		// "EPU Engine", "Dagor Engine" }) {
		// queuedPageDao.addPageToQueue(
		// mediaWikiBot
		// .queryRevisionLatest(articleTitle,
		// RevisionPropery.IDS).getPage().getId(),
		// 5000, 0);
		// }

		// QueuedPageProcessor queuedPageProcessor = appContext
		// .getBean(QueuedPageProcessor.class);
		// queuedPageProcessor.run();

		// Thread.sleep(72l * 60l * 60l * 1000l);
	}
}
