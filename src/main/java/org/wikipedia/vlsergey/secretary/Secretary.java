package org.wikipedia.vlsergey.secretary;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.wikipedia.vlsergey.secretary.books.CountBooks;
import org.wikipedia.vlsergey.secretary.books.ReplaceCiteBookWithSpecificTemplate;
import org.wikipedia.vlsergey.secretary.patrollists.BuildUnreviewedLists;
import org.wikipedia.vlsergey.secretary.trust.UpdateFeaturedArticlesTask;
import org.wikipedia.vlsergey.secretary.trust.UpdateGoodArticlesTask;
import org.wikipedia.vlsergey.secretary.trust.UpdateQualityArticlesTask;

public class Secretary {

	private static final Log log = LogFactory.getLog(Secretary.class);

	public static void main(String[] args) throws Exception {
		ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");

		// runOfType(appContext, LinkDeactivationTask.class);

		TaskScheduler taskScheduler = appContext.getBean(TaskScheduler.class);

		taskScheduler.scheduleAtFixedRate(appContext.getBean(CountBooks.class), DateUtils.MILLIS_PER_DAY);
		taskScheduler.scheduleAtFixedRate(appContext.getBean(BuildUnreviewedLists.class), DateUtils.MILLIS_PER_HOUR);

		// for (CountCiteWeb task :
		// appContext.getBeansOfType(CountCiteWeb.class).values()) {
		// taskScheduler.scheduleAtFixedRate(task, DateUtils.MILLIS_PER_DAY);
		// }

		scheduleWithFixedDelayOfType(appContext, UpdateQualityArticlesTask.class, DateUtils.MILLIS_PER_DAY);
		scheduleWithFixedDelayOfType(appContext, UpdateGoodArticlesTask.class, DateUtils.MILLIS_PER_DAY);
		scheduleWithFixedDelayOfType(appContext, UpdateFeaturedArticlesTask.class, DateUtils.MILLIS_PER_DAY);

		appContext.getBean(ReplaceCiteBookWithSpecificTemplate.class).run();

		while (true) {
			Thread.sleep(10000);
		}

	}

	private static <T extends Runnable> void runOfType(ApplicationContext appContext, Class<T> cls) {
		for (T task : appContext.getBeansOfType(cls).values()) {
			task.run();
		}
	}

	private static <T extends Runnable> void scheduleWithFixedDelayOfType(ApplicationContext appContext, Class<T> cls,
			long delay) {
		TaskScheduler taskScheduler = appContext.getBean(TaskScheduler.class);
		for (T job : appContext.getBeansOfType(cls).values()) {
			taskScheduler.scheduleWithFixedDelay(job, delay);
		}
	}
}