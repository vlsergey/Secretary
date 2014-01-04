package org.wikipedia.vlsergey.secretary.trust;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateGoodArticlesTask implements Runnable {

	private static final Log log = LogFactory.getLog(UpdateGoodArticlesTask.class);

	@Autowired
	private RevisionAuthorshipCalculator revisionAuthorshipCalculator;

	@Autowired
	private WikiStats wikiStats;

	@Override
	public void run() {
		revisionAuthorshipCalculator.updateGoodArticles();

		// for (Month month : Month.MONTHES_ALL) {
		// try {
		// wikiStats.updateByTemplateIncluded(StatisticsKey.GOOD,
		// "Шаблон:Хорошая статья", month);
		// } catch (Exception exc) {
		// log.error("Unable to update featured articles raiting: " + exc, exc);
		// }
		// }
	}

}
