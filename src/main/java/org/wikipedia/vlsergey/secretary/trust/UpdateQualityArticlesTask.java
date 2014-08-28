package org.wikipedia.vlsergey.secretary.trust;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateQualityArticlesTask implements Runnable {

	@Autowired
	private RevisionAuthorshipCalculator revisionAuthorshipCalculator;

	@Override
	public void run() {
		revisionAuthorshipCalculator.updateByTemplateIncluded("Добротные статьи", "Шаблон:Добротная статья",
				"Шаблон:Добротная статья и кандидат в хорошие");
	}

}
