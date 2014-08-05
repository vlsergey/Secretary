package org.wikipedia.vlsergey.secretary.trust;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateQualityArticlesTask implements Runnable {

	private static final Log log = LogFactory.getLog(UpdateQualityArticlesTask.class);

	@Autowired
	private RevisionAuthorshipCalculator revisionAuthorshipCalculator;

	@Override
	public void run() {
		revisionAuthorshipCalculator.updateByTemplateIncluded("Авторство добротных статей", "Добротные статьи",
				"Шаблон:Добротная статья", "Шаблон:Добротная статья и кандидат в хорошие");
	}

}
