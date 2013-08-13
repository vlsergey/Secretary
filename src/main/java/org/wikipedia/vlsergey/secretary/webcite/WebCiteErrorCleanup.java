package org.wikipedia.vlsergey.secretary.webcite;

import java.util.LinkedHashSet;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.ExternalUrl;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespaces;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

/**
 * Removes all webcite links of broken site from Wikipedia and from results
 * tables
 */
public class WebCiteErrorCleanup {

	private static final Logger logger = LoggerFactory.getLogger(WebCiteErrorCleanup.class);

	@Autowired
	private ArchivedLinkDao archivedLinkDao;

	private MediaWikiBot mediaWikiBot;

	private RefAwareParser refAwareParser;

	private WikiCache wikiCache;

	public void errorCleanup(String baseUrl) {

		archivedLinkDao.removeByAccessUrlPrefix(baseUrl);

		LinkedHashSet<Long> pageIds = new LinkedHashSet<Long>();
		for (ExternalUrl externalUrl : mediaWikiBot.queryExternalUrlUsage("http",
				StringUtils.substringAfter(baseUrl, "http://"), Namespaces.MAIN)) {
			pageIds.add(externalUrl.getPageId());
		}

		logger.debug("Found " + pageIds + " links to '" + baseUrl + "'");

		for (Revision revision : wikiCache.queryLatestContentByPageIds(pageIds)) {
			try {
				ArticleFragment article = refAwareParser.parse(revision.getXml());
				List<Template> templates = article.getAllTemplates().get("cite web");
				if (templates == null) {
					continue;
				}

				boolean hasChanges = false;
				for (Template template : templates) {
					final Content urlParameterValue = template.getParameterValue("url");
					final Content archiveurlParameterValue = template.getParameterValue("archiveurl");
					if (urlParameterValue != null
							&& archiveurlParameterValue != null
							&& StringUtils.startsWith(urlParameterValue.toWiki(true), baseUrl)
							&& StringUtils.startsWith(archiveurlParameterValue.toWiki(true),
									"http://www.webcitation.org/")) {
						template.removeParameter("archiveurl");
						template.removeParameter("archivedate");
						hasChanges = true;
					}
				}

				if (hasChanges) {
					mediaWikiBot.writeContent(revision, article.toWiki(false), "Remove broken archived links",
							true);
				}
			} catch (Exception exc) {
				logger.error("Unable to procede " + revision + ": " + exc, exc);
			}
		}
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public RefAwareParser getRefAwareParser() {
		return refAwareParser;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setRefAwareParser(RefAwareParser refAwareParser) {
		this.refAwareParser = refAwareParser;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}

}
