package org.wikipedia.vlsergey.secretary;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wikipedia.vlsergey.secretary.trust.RevisionAuthorshipCalculator;
import org.wikipedia.vlsergey.secretary.trust.WikiStats;

public class Secretary {

	public static void main(String[] args) throws Exception {
		ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");

		// TaskScheduler taskScheduler =
		// appContext.getBean(TaskScheduler.class);
		//
		// taskScheduler.scheduleAtFixedRate(appContext.getBean(CountBooks.class),
		// DateUtils.MILLIS_PER_DAY);
		// taskScheduler.scheduleAtFixedRate(appContext.getBean(BuildUnreviewedLists.class),
		// DateUtils.MILLIS_PER_HOUR);
		//
		// taskScheduler.scheduleWithFixedDelay(appContext.getBean(QueuedLinkProcessor.class),
		// DateUtils.MILLIS_PER_SECOND * 2);
		//
		// for (WebCiteJob webCiteJob :
		// appContext.getBeansOfType(WebCiteJob.class).values()) {
		// taskScheduler.scheduleWithFixedDelay(webCiteJob,
		// DateUtils.MILLIS_PER_DAY);
		// }
		//
		// for (CountCiteWeb task :
		// appContext.getBeansOfType(CountCiteWeb.class).values()) {
		// taskScheduler.scheduleAtFixedRate(task, DateUtils.MILLIS_PER_DAY);
		// }

		// appContext.getBean(ReplaceCiteBookWithSpecificTemplate.class).run();

		// appContext.getBean(RevisionAuthorshipDao.class).removeAll();
		// appContext.getBean(RevisionAuthorshipCalculator.class).updateBlockCodes();

		appContext.getBean(RevisionAuthorshipCalculator.class).updateFeaturedArticles();
		appContext.getBean(WikiStats.class).updateByTemplateIncluded(
				"Рейтинг авторов избранных статей/2013-06",
				"На данной странице делается попытка построить рейтинг редакторов избранных статей русской Википедии, "
						+ "основываясь на посещаемости статей в июне месяце и вкладе каждого редактора.",
				"Шаблон:Избранная статья");

		appContext.getBean(RevisionAuthorshipCalculator.class).updateGoodArticles();
		appContext.getBean(WikiStats.class).updateByTemplateIncluded(
				"Рейтинг авторов хороших статей/2013-06",
				"На данной странице делается попытка построить рейтинг редакторов хороших статей русской Википедии, "
						+ "основываясь на посещаемости статей в июне месяце и вкладе каждого редактора.",
				"Шаблон:Хорошая статья");

		appContext.getBean(WikiStats.class).run();

		while (true) {
			Thread.sleep(10000);
		}

	}
}
