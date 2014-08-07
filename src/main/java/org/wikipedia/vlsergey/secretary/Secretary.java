package org.wikipedia.vlsergey.secretary;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.wikipedia.vlsergey.secretary.wikidata.MoveTaxonDataToWikidata;

public class Secretary {

	private static final Log log = LogFactory.getLog(Secretary.class);

	public static void main(String[] args) throws Exception {
		ApplicationContext appContext = new ClassPathXmlApplicationContext(
				"application-context.xml");

		// runOfType(appContext, LinkDeactivationTask.class);

		// runOfType(appContext, BuildUnreviewedLists.class);
		// runOfType(appContext, UpdateQualityArticlesTask.class);
		// runOfType(appContext, UpdateGoodArticlesTask.class);
		// runOfType(appContext, UpdateFeaturedArticlesTask.class);
		// runOfType(appContext, ImportLinksFromRuWikisourceTask.class);
		// runOfType(appContext, CountBooks.class);

		// scheduleWithFixedDelayOfType(appContext, CountBooks.class,
		// DateUtils.MILLIS_PER_DAY);
		// scheduleWithFixedDelayOfType(appContext, BuildUnreviewedLists.class,
		// DateUtils.MILLIS_PER_HOUR);
		// scheduleWithFixedDelayOfType(appContext,
		// UpdateQualityArticlesTask.class, DateUtils.MILLIS_PER_DAY);
		// scheduleWithFixedDelayOfType(appContext,
		// UpdateGoodArticlesTask.class, DateUtils.MILLIS_PER_DAY);
		// scheduleWithFixedDelayOfType(appContext,
		// UpdateFeaturedArticlesTask.class, DateUtils.MILLIS_PER_DAY);

		appContext.getBean(MoveTaxonDataToWikidata.class).run();

		// appContext.getBean(ReplaceCiteBookWithSpecificTemplate.class).run();
		// appContext.getBean(ImportLinksFromRuWikisourceTask.class).run();

		// while (true) {
		// Thread.sleep(10000);
		// }
	}

	private static <T extends Runnable> void runOfType(
			ApplicationContext appContext, Class<T> cls) {
		for (T task : appContext.getBeansOfType(cls).values()) {
			task.run();
		}
	}

	private static <T extends Runnable> void scheduleWithFixedDelayOfType(
			ApplicationContext appContext, Class<T> cls, long delay) {
		TaskScheduler taskScheduler = appContext.getBean(TaskScheduler.class);
		for (T job : appContext.getBeansOfType(cls).values()) {
			taskScheduler.scheduleWithFixedDelay(job, delay);
		}
	}
}