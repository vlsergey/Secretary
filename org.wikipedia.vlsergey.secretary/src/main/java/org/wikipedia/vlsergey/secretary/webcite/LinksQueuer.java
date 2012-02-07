package org.wikipedia.vlsergey.secretary.webcite;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.parser.Parser;
import org.wikipedia.vlsergey.secretary.dom.parser.ParsingException;
import org.wikipedia.vlsergey.secretary.http.HttpManager;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class LinksQueuer {
	private static final Logger logger = LoggerFactory
			.getLogger(LinksQueuer.class);

	@Autowired
	private ArchivedLinkDao archivedLinkDao;

	@Autowired
	private ArticleLinksCollector articleLinksCollector;

	@Autowired
	private MediaWikiBot mediaWikiBot;

	@Autowired
	private QueuedLinkDao queuedLinkDao;

	@Autowired
	private WebCiteArchiver webCiteArchiver;

	@Autowired
	private WikiCache wikiCache;

	public boolean isQueuedOrArchived(String url, String accessDate) {
		return archivedLinkDao.findNonBrokenLink(url, accessDate) != null
				|| queuedLinkDao.findLink(url, accessDate) != null;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public ArchivedLink queueOrGetResult(ArticleLink articleLink,
			long newPriority) {
		ArchivedLink archivedLink = archivedLinkDao.findNonBrokenLink(
				articleLink.url, articleLink.accessDate);

		if (archivedLink != null)
			return archivedLink;

		QueuedLink queuedLink = new QueuedLink();
		queuedLink.setAccessDate(articleLink.accessDate);
		queuedLink.setArticleDate(articleLink.articleDate);
		queuedLink.setAuthor(articleLink.author);
		queuedLink.setTitle(articleLink.title);
		queuedLink.setPriority(newPriority);
		queuedLink.setUrl(articleLink.url);
		queuedLinkDao.addLinkToQueue(queuedLink);

		return null;
	}

	@Transactional(propagation = Propagation.REQUIRED)
	public void saveArchivedLink(ArticleLink articleLink)
			throws ClientProtocolException, IOException {
		ArchivedLink archivedLink = new ArchivedLink();
		archivedLink.setAccessDate(articleLink.accessDate);
		archivedLink.setAccessUrl(articleLink.url);
		archivedLink.setArchiveDate(articleLink.archiveDate);

		String status;
		if (archivedLink.getAccessUrl().startsWith(
				"http://www.webcitation.org/")) {
			status = webCiteArchiver.getStatus(HttpManager.DEFAULT_CLIENT,
					StringUtils.substringAfter(archivedLink.getAccessUrl(),
							"http://www.webcitation.org/"));
		} else {
			// what else may be in article already?
			status = ArchivedLink.STATUS_SUCCESS;
		}
		archivedLink.setArchiveResult(status);

		archivedLink.setArchiveUrl(articleLink.archiveUrl);

		archivedLinkDao.persist(archivedLink);
	}

	@Transactional(propagation = Propagation.NEVER)
	public void storeArchivedLinksFromArticle(Long pageId) throws Exception {
		Revision latestRevision = wikiCache.queryLatestRevision(pageId);
		if (latestRevision == null) {
			logger.warn("No latest revision for page #" + pageId);
			return;
		}

		String latestContent = latestRevision.getContent();

		if (StringUtils.isEmpty(latestContent)) {
			logger.warn("No content for revision #" + latestRevision.getId()
					+ " of page #" + pageId + " ('"
					+ latestRevision.getPage().getTitle() + "')");
			return;
		}

		try {
			storeArchivedLinksFromArticleContent(latestContent);
		} catch (ParsingException exc) {
			logger.warn("Parsing exception occur during processing revision #"
					+ latestRevision.getId() + " of page #" + pageId + " ('"
					+ latestRevision.getPage().getTitle() + "'): " + exc, exc);
		}
	}

	@Transactional(propagation = Propagation.NEVER)
	public void storeArchivedLinksFromArticle(String articleName)
			throws Exception {
		Revision latestRevision = wikiCache.queryLatestRevision(articleName);
		if (latestRevision == null) {
			logger.warn("No latest revision for page '" + articleName + "'");
			return;
		}

		String latestContent = wikiCache
				.queryLatestRevisionContent(articleName);

		if (StringUtils.isEmpty(latestContent))
			return;

		try {
			storeArchivedLinksFromArticleContent(latestContent);
		} catch (ParsingException exc) {
			logger.warn("Parsing exception occur during processing revision #"
					+ latestRevision.getId() + " of page #"
					+ latestRevision.getPage().getId() + " ('" + articleName
					+ "'): " + exc, exc);
		}
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public void storeArchivedLinksFromArticleContent(String latestContent)
			throws Exception {
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

			if (archivedLinkDao.findNonBrokenLink(articleLink.url,
					articleLink.accessDate) != null) {
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
