package org.wikipedia.vlsergey.secretary.webcite;

import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.ExternalUrl;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespaces;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class LinkDeactivationTask implements Runnable {

	private MediaWikiBot mediaWikiBot;

	private WikiCache wikiCache;

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	@Override
	public void run() {
		final String host = "dic.academic.ru";

		for (ExternalUrl externalUrl : mediaWikiBot.queryExternalUrlUsage("http", host, Namespaces.USER)) {
			if (externalUrl.getPageTitle().startsWith("Участник:" + mediaWikiBot.getLogin() + "/")) {

				Revision revision = wikiCache.queryLatestRevision(externalUrl.getPageId());
				String content = revision.getContent();

				content = StringUtils.replace(content, "http://" + host, host);
				mediaWikiBot.writeContent(revision, content, "Deactivation of '" + host + "' links", true);
			}
		}
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}

}
