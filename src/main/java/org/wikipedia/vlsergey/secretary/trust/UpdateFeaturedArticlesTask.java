package org.wikipedia.vlsergey.secretary.trust;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateFeaturedArticlesTask implements Runnable {

	private static final Log log = LogFactory.getLog(UpdateFeaturedArticlesTask.class);

	@Autowired
	private RevisionAuthorshipCalculator revisionAuthorshipCalculator;

	@Autowired
	private WikiStats wikiStats;

	@Override
	public void run() {
		revisionAuthorshipCalculator.updateFeaturedArticles();

		try {
			wikiStats.updateByTemplateIncluded(StatisticsKey.FEATURED, "Шаблон:Избранная статья",
					Month.MONTH_OF_2013_06);
		} catch (Exception exc) {
			log.error("Unable to update featured articles raiting: " + exc, exc);
		}

		try {
			wikiStats.updateByTemplateIncluded(StatisticsKey.FEATURED, "Шаблон:Избранная статья",
					Month.MONTH_OF_2013_07);
		} catch (Exception exc) {
			log.error("Unable to update featured articles raiting: " + exc, exc);
		}
	}

}
