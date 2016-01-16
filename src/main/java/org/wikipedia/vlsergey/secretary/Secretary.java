package org.wikipedia.vlsergey.secretary;

import org.apache.commons.lang3.time.DateUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.wikipedia.vlsergey.secretary.books.CountBooks;
import org.wikipedia.vlsergey.secretary.patrollists.BuildUnreviewedLists;
import org.wikipedia.vlsergey.secretary.trust.UpdateGoodArticlesTask;
import org.wikipedia.vlsergey.secretary.trust.UpdateQualityArticlesTask;
import org.wikipedia.vlsergey.secretary.wikidata.CompareWithWikidata;
import org.wikipedia.vlsergey.secretary.wikidata.DictinaryUpdate;

public class Secretary {

	// private static final Log log = LogFactory.getLog(Secretary.class);

	public static void main(String[] args) throws Exception {
		ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");

		// runOfType(appContext, LinkDeactivationTask.class);

		// runOfType(appContext, BuildUnreviewedLists.class);
		// runOfType(appContext, UpdateQualityArticlesTask.class);
		// runOfType(appContext, UpdateGoodArticlesTask.class);
		// runOfType(appContext, UpdateFeaturedArticlesTask.class);
		// runOfType(appContext, ImportLinksFromRuWikisourceTask.class);
		// runOfType(appContext, CountBooks.class);

		// scheduleWithFixedDelayOfType(appContext, ChangeQualifierType.class,
		// DateUtils.MILLIS_PER_DAY);
		// scheduleWithFixedDelayOfType(appContext,
		// ConstrainCheckerPeriod.class, DateUtils.MILLIS_PER_HOUR * 12);
		// scheduleWithFixedDelayOfType(appContext,
		// ConstrainCheckerQualifiers.class, DateUtils.MILLIS_PER_HOUR * 12);
		// scheduleWithFixedDelayOfType(appContext, DictinaryFlagsUpdate.class,
		// DateUtils.MILLIS_PER_HOUR);
		// scheduleWithFixedDelayOfType(appContext, EnumerateProperties.class,
		// DateUtils.MILLIS_PER_HOUR);
		// scheduleWithFixedDelayOfType(appContext, MoveDataToWikidata.class,
		// DateUtils.MILLIS_PER_HOUR * 12);

		scheduleWithFixedDelayOfType(appContext, BuildUnreviewedLists.class, DateUtils.MILLIS_PER_HOUR);
		scheduleWithFixedDelayOfType(appContext, CompareWithWikidata.class, DateUtils.MILLIS_PER_DAY);
		scheduleWithFixedDelayOfType(appContext, CountBooks.class, DateUtils.MILLIS_PER_DAY);
		scheduleWithFixedDelayOfType(appContext, DictinaryUpdate.class, DateUtils.MILLIS_PER_HOUR);
		scheduleWithFixedDelayOfType(appContext, UpdateQualityArticlesTask.class, DateUtils.MILLIS_PER_HOUR);
		scheduleWithFixedDelayOfType(appContext, UpdateGoodArticlesTask.class, DateUtils.MILLIS_PER_HOUR);
		// scheduleWithFixedDelayOfType(appContext,
		// UpdateFeaturedArticlesTask.class, DateUtils.MILLIS_PER_HOUR);

		// ((WikiCache) appContext.getBean("wikidataCache")).clear();
		// ((WikiCache) appContext.getBean("ruWikipediaCache")).clear();

		// runOfType(appContext, UndoVlsergeyBotWork.class);

		// runOfType(appContext, ChangeQualifierType.class);
		// runOfType(appContext, DictinaryUpdate.class);
		// runOfType(appContext, EnumerateProperties.class);
		// runOfType(appContext, CalculateCountries.class);
		// runOfType(appContext, MoveDataToWikidata.class);
		// runOfType(appContext, CalculateCountries.class);
		// runOfType(appContext, ZeroEdits.class);
		// runOfType(appContext, MoveCommonsCategoryToWikidata.class);
		// runOfType(appContext, MoveDataToWikidata.class);

		// appContext.getBean(ReplaceCiteBookWithSpecificTemplate.class).run();
		// appContext.getBean(ImportLinksFromRuWikisourceTask.class).run();

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