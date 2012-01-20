package org.wikipedia.vlsergey.secretary.webcite;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.parser.Parser;
import org.wikipedia.vlsergey.secretary.http.HttpManager;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.ExternalUrl;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

@Component
public class WebCiteChecker {

	private static final Log logger = LogFactory.getLog(WebCiteChecker.class);

	@Autowired
	private ArchivedLinkDao archivedLinkDao;

	@Autowired
	private ArticleLinksCollector articleLinksCollector;

	@Autowired
	private HttpManager httpManager;

	@Autowired
	private MediaWikiBot mediaWikiBot;

	@Autowired
	private WebCiteArchiver webCiteArchiver;

	@Autowired
	private WikiCache wikiCache;

	private boolean isInvalid(String archiveUrl) {
		List<ArchivedLink> inDb = archivedLinkDao.findByArchiveUrl(archiveUrl);

		boolean invalid = false;
		for (ArchivedLink storedLink : inDb) {
			if (StringUtils.contains(storedLink.getArchiveResult(),
					"Invalid snapshot ID")) {
				invalid = true;
				break;
			}
		}

		return invalid;
	}

	private void processArticle(Long pageId) {
		Revision revision = wikiCache.queryLatestRevision(pageId);
		String latestContent = revision.getContent();
		ArticleFragment latestContentDom = new Parser().parse(latestContent);

		boolean hasChanges = false;
		for (ArticleLink articleLink : articleLinksCollector
				.getAllLinks(latestContentDom)) {
			String archiveUrl = articleLink.archiveUrl;

			if (!StringUtils.startsWith(archiveUrl,
					"http://www.webcitation.org/"))
				continue;

			if (!isInvalid(archiveUrl))
				continue;

			final boolean multilineFormat = articleLink.template.toString()
					.contains("\n");

			articleLink.template
					.removeParameter(WikiConstants.PARAMETER_ARCHIVEDATE);
			articleLink.template
					.removeParameter(WikiConstants.PARAMETER_ARCHIVEURL);
			articleLink.template.format(multilineFormat, multilineFormat);
			hasChanges = true;

		}

		if (hasChanges) {
			mediaWikiBot.writeContent(revision.getPage(), revision,
					latestContentDom.toString(),
					"Removing broken WebCite links", true, true);
		}
	}

	public void run() throws Exception {
		// String clientCode = httpManager.getClientCodes().iterator().next();

		// for (ArchivedLink archivedLink : archivedLinkDao
		// .findByArchiveResult("")) {
		// try {
		// String webCiteCode = StringUtils.substringAfter(
		// archivedLink.getArchiveUrl(),
		// "http://www.webcitation.org/");
		// if (StringUtils.isEmpty(webCiteCode))
		// continue;
		//
		// if (!StringUtils.isAlphanumeric(webCiteCode))
		// continue;
		//
		// String archiveResult = webCiteArchiver.getStatus(clientCode,
		// webCiteCode);
		// archivedLink.setArchiveResult(archiveResult);
		// archivedLinkDao.setArchiveResult(archivedLink, archiveResult);
		//
		// Thread.sleep(500);
		// } catch (Exception exc) {
		// logger.error(exc, exc);
		// }
		// }

		Set<Long> pages = new TreeSet<Long>();
		for (ExternalUrl externalUrl : mediaWikiBot.queryExternalUrlUsage(
				"http", "www.webcitation.org", 0)) {

			if (!StringUtils.startsWith(externalUrl.getUrl(),
					"http://www.webcitation.org/"))
				continue;

			if (!isInvalid(externalUrl.getUrl()))
				continue;

			pages.add(externalUrl.getPageId());
		}

		final long start = System.currentTimeMillis();
		final int size = pages.size();
		int counter = 0;
		for (Long page : pages) {
			try {
				processArticle(page);
			} catch (Exception exc) {
				logger.error("Unable to process article #" + page + ": " + exc,
						exc);
			}
			counter++;
			long expectedEnd = start + (System.currentTimeMillis() - start)
					* size / counter;
			logger.info("Done " + counter + " from " + size
					+ ". Expected finish time is " + new Date(expectedEnd));
		}
	}
}
