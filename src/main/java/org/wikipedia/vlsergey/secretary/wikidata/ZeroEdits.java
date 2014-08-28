package org.wikipedia.vlsergey.secretary.wikidata;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Component
public class ZeroEdits implements Runnable {

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	@Override
	public void run() {
		for (Revision revision : ruWikipediaCache.queryByBacklinks(1908172l, Namespace.NSS_MAIN)) {
			try {
				ruWikipediaCache.getMediaWikiBot().writeContent(revision, revision.getContent(), "zero edit", true);
			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}
	}

}
