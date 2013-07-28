package org.wikipedia.vlsergey.secretary.webcite;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.http.HttpManager;

public class QueuedLinkProcessor implements Runnable {
	private static final Logger logger = LoggerFactory.getLogger(QueuedLinkProcessor.class);

	@Autowired
	private ArchivedLinkDao archivedLinkDao;

	@Autowired
	private HttpManager httpManager;

	@Autowired
	private QueuedLinkDao queuedLinkDao;

	@Autowired
	private WebCiteArchiver webCiteArchiver;

	@Autowired
	private WebCiteLimiter webCiteLimiter;

	public void clearQueue() {
		logger.info("Removing all links from queue...");
		queuedLinkDao.removeAll();
	}

	@Override
	public void run() {
		try {
			runImpl();
		} catch (Throwable exc) {
			logger.error("" + exc, exc);
		}
	}

	private boolean runImpl() throws Exception {
		QueuedLink queuedLink = queuedLinkDao.getLinkFromQueue();
		if (queuedLink == null) {
			logger.trace("No links in queue. Sleep.");
			return false;
		}

		try {
			// is it in ignore list already?
			if (QueuedPageProcessor.ignoreUrl(null, queuedLink.getUrl())) {
				// shall be skipped
				queuedLinkDao.removeLinkFromQueue(queuedLink);
				return true;
			}

			// are we sure there is no match?
			if (archivedLinkDao.findNonBrokenLink(queuedLink.getUrl(), queuedLink.getAccessDate()) != null) {
				// already processed
				queuedLinkDao.removeLinkFromQueue(queuedLink);
				return true;
			}

			// make sure we made at least 5 second pause between requests
			logger.debug("Sleeping 5 seconds...");
			Thread.sleep(5 * 1000);

			String allowedClient = null;
			while (allowedClient == null) {
				for (String clientCode : httpManager.getClientCodes()) {
					if (webCiteLimiter.isAllowed(clientCode)) {
						allowedClient = clientCode;
						break;
					}
				}

				if (allowedClient == null) {
					Thread.sleep(10 * 60 * 1000);
				}
			}

			assert allowedClient != null;
			assert webCiteLimiter.isAllowed(allowedClient);

			// should be allowed now
			webCiteLimiter.beforeRequest(allowedClient);
			String archiveUrl = webCiteArchiver.archive(allowedClient, queuedLink.getUrl(), queuedLink.getTitle(),
					queuedLink.getAuthor(), queuedLink.getArticleDate());

			String webCiteCode = StringUtils.substringAfterLast(archiveUrl, "/");
			logger.debug("WebCite code is '" + webCiteCode + "'");

			String status = webCiteArchiver.getStatus(allowedClient, webCiteCode);

			for (QueuedLink sameUrl : queuedLinkDao.findByUrl(queuedLink.getUrl())) {
				logger.debug("Creating new archived record for '" + sameUrl.getUrl() + "' and date '"
						+ sameUrl.getAccessDate() + "'");

				ArchivedLink archivedLink = new ArchivedLink();
				archivedLink.setAccessDate(StringUtils.trimToEmpty(sameUrl.getAccessDate()));
				archivedLink.setAccessUrl(StringUtils.trimToEmpty(sameUrl.getUrl()));
				archivedLink.setArchiveDate(DateFormatUtils.format(new Date(), "yyyy-MM-dd"));
				archivedLink.setArchiveResult(StringUtils.trimToEmpty(status));
				archivedLink.setArchiveUrl(StringUtils.trimToEmpty(archiveUrl));
				archivedLinkDao.persist(archivedLink);

				queuedLinkDao.removeLinkFromQueue(sameUrl);
			}

			return true;
		} catch (Exception exc) {
			queuedLinkDao.reducePriority(queuedLink);
			throw exc;
		}
	}
}
