package org.wikipedia.vlsergey.secretary.webcite;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.ExternalUrl;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespaces;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class LinkDeactivationTask implements Runnable {

	private static final Log log = LogFactory.getLog(LinkDeactivationTask.class);

	private MediaWikiBot mediaWikiBot;

	private WikiCache wikiCache;

	private void deactivateLink(final String host) {
		for (ExternalUrl externalUrl : mediaWikiBot.queryExternalUrlUsage("http", host, Namespaces.USER)) {
			final String pageTitle = externalUrl.getPageTitle();
			if (pageTitle.contains(":" + mediaWikiBot.getLogin() + "/")) {
				try {
					Revision revision = wikiCache.queryLatestRevision(externalUrl.getPageId());
					String content = revision.getContent();

					content = StringUtils.replace(content, "http://" + host, host);
					mediaWikiBot.writeContent(revision, content, "Deactivation of '" + host + "' links", true);
				} catch (Exception exc) {
					log.error("Unable to deactivate links to '" + host + "' on page " + pageTitle + ": " + exc, exc);
				}
			}
		}
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	@Override
	public void run() {
		deactivateLink("pseudology.org");
		deactivateLink("www.pseudology.org");

		deactivateLink("dic.academic.ru");
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}

}
