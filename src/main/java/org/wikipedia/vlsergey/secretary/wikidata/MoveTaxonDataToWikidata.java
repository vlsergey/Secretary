package org.wikipedia.vlsergey.secretary.wikidata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

public class MoveTaxonDataToWikidata implements Runnable {

	@Autowired
	@Qualifier("ruWikipediaBot")
	private MediaWikiBot ruWikipediaBot;

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	@Override
	public void run() {
		// TODO Auto-generated method stub

		for (Revision revision : ruWikipediaCache.queryByEmbeddedIn("Шаблон:Таксон", (Namespace[]) null)) {
			revision.get
		}

	}
}
