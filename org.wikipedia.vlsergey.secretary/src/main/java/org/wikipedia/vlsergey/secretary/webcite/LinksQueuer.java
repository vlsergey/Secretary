package org.wikipedia.vlsergey.secretary.webcite;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.parser.Parser;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

public class LinksQueuer {
	private static final Logger logger = LoggerFactory
			.getLogger(LinksQueuer.class);

	@Autowired
	private ArchivedLinkDao archivedLinkDao;

	@Autowired
	private QueuedLinkDao queuedLinkDao;

	@Autowired
	private ArticleLinksCollector articleLinksCollector;

	@Autowired
	private MediaWikiBot mediaWikiBot;

	@Autowired
	private WikiCache wikiCache;

	@Transactional(propagation = Propagation.REQUIRED)
	public void saveArchivedLink(ArticleLink articleLink) {
		ArchivedLink archivedLink = new ArchivedLink();
		archivedLink.setAccessDate(articleLink.accessDate);
		archivedLink.setAccessUrl(articleLink.url);
		archivedLink.setArchiveDate(articleLink.archiveDate);
		// what else may be in article already?
		archivedLink.setArchiveResult("success");
		archivedLink.setArchiveUrl(articleLink.archiveUrl);

		archivedLinkDao.persist(archivedLink);
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public ArchivedLink queueOrGetResult(ArticleLink articleLink) {
		List<ArchivedLink> archivedLinks = archivedLinkDao.getArchivedLinks(
				articleLink.url, articleLink.accessDate);

		if (archivedLinks.size() > 2) {
			logger.error("More than one URLs with same access date ("
					+ articleLink.accessDate + ") and URL '" + articleLink.url
					+ "'");
			throw new IllegalStateException();
		}

		if (!archivedLinks.isEmpty()) {
			return archivedLinks.get(0);
		}

		QueuedLink queuedLink = new QueuedLink();
		queuedLink.setAccessDate(articleLink.accessDate);
		queuedLink.setArticleDate(articleLink.articleDate);
		queuedLink.setAuthor(articleLink.author);
		queuedLink.setTitle(articleLink.title);
		queuedLink.setUrl(articleLink.url);
		queuedLinkDao.addLinkToQueue(queuedLink);

		return null;
	}

	@Transactional(propagation = Propagation.NEVER)
	public void storeArchivedLinksFromArticle(Long pageId) throws Exception {
		String latestContent = wikiCache.queryLatestRevisionContent(pageId);

		if (StringUtils.isEmpty(latestContent))
			return;

		storeArchivedLinksFromArticleContent(latestContent);
	}

	@Transactional(propagation = Propagation.NEVER)
	public void storeArchivedLinksFromArticle(String articleName)
			throws Exception {
		String latestContent = wikiCache
				.queryLatestRevisionContent(articleName);

		if (StringUtils.isEmpty(latestContent))
			return;

		storeArchivedLinksFromArticleContent(latestContent);
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public void storeArchivedLinksFromArticleContent(String latestContent) {
		ArticleFragment articleFragment = new Parser().parse(latestContent);
		List<ArticleLink> allLinks = articleLinksCollector
				.getAllLinks(articleFragment);

		// for now - just save...
		for (ArticleLink articleLink : allLinks) {

			String originalUrl = StringUtils.trimToEmpty(articleLink.url);
			if (StringUtils.isEmpty(originalUrl))
				continue;

			if (StringUtils.isEmpty(articleLink.archiveUrl))
				continue;

			// we have URL that can save for use in other articles
			URI originalUri;
			try {
				originalUri = URI.create(originalUrl);
			} catch (IllegalArgumentException exc) {
				logger.warn("URL " + originalUrl
						+ " skipped due wrong format: " + exc.getMessage());
				continue;
			}

			String originalHost = originalUri.getHost().toLowerCase();
			if (WebCiteArchiver.SKIP_ERRORS.contains(originalHost)
					|| WebCiteArchiver.SKIP_TECH_LIMITS.contains(originalHost)) {
				// we can have archived copy, but it may be incorrect
				// this archived copy is not on archives site
				logger.warn("Ignoring original URL '" + originalUrl
						+ "' because even if copy exists, it may be incorrect");
				continue;
			}

			String archiveUrl = StringUtils.trimToEmpty(articleLink.archiveUrl);
			if (StringUtils.isEmpty(archiveUrl))
				continue;

			URI archiveUri;
			try {
				archiveUri = URI.create(archiveUrl);
			} catch (IllegalArgumentException exc) {
				logger.warn("URL " + archiveUrl + " skipped due wrong format: "
						+ exc.getMessage());
				continue;
			}
			String archiveHost = archiveUri.getHost().toLowerCase();

			if (!WebCiteArchiver.SKIP_ARCHIVES.contains(archiveHost)) {
				// this archived copy is not on archives site
				logger.warn("Ignoring archive URL '" + archiveUrl
						+ "' (as archive copy of '" + originalUrl
						+ "') because unknown archive site");
				continue;
			}

			if (!archivedLinkDao.getArchivedLinks(articleLink.url,
					articleLink.accessDate).isEmpty()) {
				// already have such link in out DB
				logger.debug("Ignoring archive URL '" + archiveUrl
						+ "' (as archive copy of '" + originalUrl
						+ "') because already stored in DB");
				continue;
			}

			saveArchivedLink(articleLink);
		}
	}

	@Transactional(propagation = Propagation.NEVER)
	public void storeArchivedLinksFromWikipedia() throws Exception {

		Collection<Long> pageIds = new HashSet<Long>();
		for (Long pageId : mediaWikiBot
				.queryEmbeddedInPageIds("Шаблон:Cite web")) {
			pageIds.add(pageId);
		}
		pageIds = new ArrayList<Long>(pageIds);
		Collections.sort((List<Long>) pageIds);

		for (Revision revision : wikiCache.queryLatestContentByPageIdsF()
				.batchlazy(50).apply(pageIds)) {
			logger.debug("Processing links from revision #" + revision.getId()
					+ " of article #" + revision.getPage().getId() + " ('"
					+ revision.getPage().getTitle() + "')");

			try {
				String content = revision.getContent();

				if (StringUtils.isEmpty(content))
					return;

				storeArchivedLinksFromArticleContent(content);
			} catch (Exception exc) {
				logger.error("Unable to process links from revision #"
						+ revision.getId() + " of article #"
						+ revision.getPage().getId() + " ('"
						+ revision.getPage().getTitle() + "'):" + exc);
			}
		}
	}

}
