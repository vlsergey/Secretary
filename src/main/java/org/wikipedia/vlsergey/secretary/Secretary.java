package org.wikipedia.vlsergey.secretary;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.TaskScheduler;
import org.wikipedia.vlsergey.secretary.books.CountBooks;
import org.wikipedia.vlsergey.secretary.books.ReplaceCiteBookWithSpecificTemplate;
import org.wikipedia.vlsergey.secretary.patrollists.BuildUnreviewedLists;
import org.wikipedia.vlsergey.secretary.webcite.WebCiteJob;

public class Secretary {

	public static void main(String[] args) throws Exception {
		ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");

		TaskScheduler taskScheduler = appContext.getBean(TaskScheduler.class);

		taskScheduler.scheduleWithFixedDelay(appContext.getBean(CountBooks.class), DateUtils.MILLIS_PER_DAY);
		taskScheduler.scheduleWithFixedDelay(appContext.getBean(BuildUnreviewedLists.class), DateUtils.MILLIS_PER_HOUR);
		taskScheduler.scheduleWithFixedDelay(appContext.getBean(WebCiteJob.class), DateUtils.MILLIS_PER_DAY);

		appContext.getBean(ReplaceCiteBookWithSpecificTemplate.class).run();

		while (true) {
			Thread.sleep(10000);
		}

	}
}
