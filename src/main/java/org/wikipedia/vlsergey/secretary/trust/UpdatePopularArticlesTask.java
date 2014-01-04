package org.wikipedia.vlsergey.secretary.trust;

import org.apache.commons.lang.time.DateUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

@Component
public class UpdatePopularArticlesTask implements Runnable {

	private static final Log log = LogFactory.getLog(UpdatePopularArticlesTask.class);

	@Autowired
	private RevisionAuthorshipCalculator revisionAuthorshipCalculator;

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private WikiStats wikiStats;

	@Override
	public void run() {
		for (Month month : Month.MONTHES_ALL) {
			try {
				wikiStats.run(month);
			} catch (Exception exc) {
				log.error("Unable to update popular articles raiting: " + exc, exc);
			}
		}
	}

	public void schedule() {
		for (final Month month : Month.MONTHES_ALL) {
			taskScheduler.scheduleWithFixedDelay(new Runnable() {
				@Override
				public void run() {
					try {
						wikiStats.run(month);
					} catch (Exception exc) {
						log.error("Unable to update popular articles raiting: " + exc, exc);
					}
				}
			}, DateUtils.MILLIS_PER_HOUR);
		}
	}

}
