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

	@Override
	public void run() {
		revisionAuthorshipCalculator.updateByTemplateIncluded("Авторство хороших статей", "Хорошие статьи",
				"Шаблон:Хорошая статья", "Шаблон:Хорошая статья и кандидат в избранные");
	}

}
