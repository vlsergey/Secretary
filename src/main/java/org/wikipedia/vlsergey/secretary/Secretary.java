package org.wikipedia.vlsergey.secretary;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.wikipedia.vlsergey.secretary.books.CountBooks;
import org.wikipedia.vlsergey.secretary.patrollists.BuildUnreviewedLists;
import org.wikipedia.vlsergey.secretary.trust.Month;
import org.wikipedia.vlsergey.secretary.trust.UpdateFeaturedArticlesTask;
import org.wikipedia.vlsergey.secretary.trust.UpdateGoodArticlesTask;
import org.wikipedia.vlsergey.secretary.trust.WikiStats;
import org.wikipedia.vlsergey.secretary.webcite.QueuedLinkProcessor;
import org.wikipedia.vlsergey.secretary.webcite.WebCiteJob;

public class Secretary {

	private static final Log log = LogFactory.getLog(Secretary.class);

	public static void main(String[] args) throws Exception {
		ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");

		// runOfType(appContext, LinkDeactivationTask.class);

		TaskScheduler taskScheduler = appContext.getBean(TaskScheduler.class);

		taskScheduler.scheduleAtFixedRate(appContext.getBean(CountBooks.class), DateUtils.MILLIS_PER_DAY);
		taskScheduler.scheduleAtFixedRate(appContext.getBean(BuildUnreviewedLists.class), DateUtils.MILLIS_PER_HOUR);

		taskScheduler.scheduleWithFixedDelay(appContext.getBean(QueuedLinkProcessor.class),
				DateUtils.MILLIS_PER_SECOND * 2);

		for (WebCiteJob webCiteJob : appContext.getBeansOfType(WebCiteJob.class).values()) {
			taskScheduler.scheduleWithFixedDelay(webCiteJob, DateUtils.MILLIS_PER_DAY);
		}

		// for (CountCiteWeb task :
		// appContext.getBeansOfType(CountCiteWeb.class).values()) {
		// taskScheduler.scheduleAtFixedRate(task, DateUtils.MILLIS_PER_DAY);
		// }

		final UpdateFeaturedArticlesTask updateFeaturedArticlesTask = appContext
				.getBean(UpdateFeaturedArticlesTask.class);
		final UpdateGoodArticlesTask updateGoodArticlesTask = appContext.getBean(UpdateGoodArticlesTask.class);

		updateFeaturedArticlesTask.run();
		updateGoodArticlesTask.run();

		taskScheduler.scheduleWithFixedDelay(updateFeaturedArticlesTask, DateUtils.MILLIS_PER_DAY);
		taskScheduler.scheduleWithFixedDelay(updateGoodArticlesTask, DateUtils.MILLIS_PER_DAY);

		final WikiStats wikiStats = appContext.getBean(WikiStats.class);

		// if exception happens -- restart in one hour
		taskScheduler.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					wikiStats.run(Month.MONTH_OF_2013_06, DateUtils.MILLIS_PER_MINUTE * 10, DateUtils.MILLIS_PER_HOUR);
				} catch (Exception exc) {
					log.error("Unable to update total raiting: " + exc, exc);
				}
			}
		}, DateUtils.MILLIS_PER_HOUR);

		taskScheduler.scheduleWithFixedDelay(new Runnable() {
			@Override
			public void run() {
				try {
					wikiStats.run(Month.MONTH_OF_2013_07, DateUtils.MILLIS_PER_MINUTE * 15, DateUtils.MILLIS_PER_HOUR);
				} catch (Exception exc) {
					log.error("Unable to update total raiting: " + exc, exc);
				}
			}
		}, DateUtils.MILLIS_PER_HOUR);

		// appContext.getBean(ReplaceCiteBookWithSpecificTemplate.class).run();

		while (true) {
			Thread.sleep(10000);
		}

	}

	private static <T extends Runnable> void runOfType(ApplicationContext appContext, Class<T> cls) {
		for (T task : appContext.getBeansOfType(cls).values()) {
			task.run();
		}
	}
}