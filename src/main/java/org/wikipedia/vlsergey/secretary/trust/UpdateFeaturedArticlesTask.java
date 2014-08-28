package org.wikipedia.vlsergey.secretary.trust;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UpdateFeaturedArticlesTask implements Runnable {

	@Autowired
	private RevisionAuthorshipCalculator revisionAuthorshipCalculator;

	@Override
	public void run() {
		revisionAuthorshipCalculator.updateByTemplateIncluded("Избранные статьи", "Шаблон:Избранная статья");
	}

}
