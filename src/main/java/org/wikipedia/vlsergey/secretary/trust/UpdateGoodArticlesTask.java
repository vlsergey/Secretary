package org.wikipedia.vlsergey.secretary.trust;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateGoodArticlesTask implements Runnable {

	@Autowired
	private RevisionAuthorshipCalculator revisionAuthorshipCalculator;

	@Override
	public void run() {
		revisionAuthorshipCalculator.updateByTemplateIncluded("Хорошие статьи", "Шаблон:Хорошая статья",
				"Шаблон:Хорошая статья и кандидат в избранные");
	}

}
