package org.wikipedia.vlsergey.secretary.webcite;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class QueuedLinkProcessor {
	private static final Logger logger = LoggerFactory
			.getLogger(QueuedLinkProcessor.class);

	@Autowired
	private ArchivedLinkDao archivedLinkDao;

	@Autowired
	private QueuedLinkDao queuedLinkDao;

	@Autowired
	private WebCiteArchiver webCiteArchiver;

	@Autowired
	private WebCiteLimiter webCiteLimiter;

	protected void run() {
		while (true) {
			try {
				boolean didAnyWork = runImpl();

				if (!didAnyWork)
					Thread.sleep(5000);
			} catch (Throwable exc) {
				logger.error("" + exc, exc);
			}
		}
	}

	private boolean runImpl() throws Exception {
		QueuedLink queuedLink = queuedLinkDao.getLinkFromQueue();
		if (queuedLink == null)
			return false;

		// are we sure there is no match?
		if (archivedLinkDao.findLink(queuedLink.getUrl(),
				queuedLink.getAccessDate()) != null) {
			// already processed
			queuedLinkDao.removeLinkFromQueue(queuedLink);
			return true;
		}

		while (!webCiteLimiter.isAllowed()) {
			logger.debug("Sleeping until 10 minutes...");
			Thread.sleep(10 * 60 * 1000);
		}

		// make sure we made at least 5 second pause between requests
		Thread.sleep(5 * 1000);

		assert webCiteLimiter.isAllowed();

		// should be allowed now
		webCiteLimiter.beforeRequest();
		String archiveUrl = webCiteArchiver.archive(queuedLink.getUrl(),
				queuedLink.getTitle(), queuedLink.getAuthor(),
				queuedLink.getArticleDate());

		String webCiteCode = StringUtils.substringAfterLast(archiveUrl, "/");
		logger.debug("WebCite code is '" + webCiteCode + "'");

		String status = webCiteArchiver.getStatus(webCiteCode);

		ArchivedLink archivedLink = new ArchivedLink();
		archivedLink.setAccessDate(StringUtils.trimToEmpty(queuedLink
				.getAccessDate()));
		archivedLink.setAccessUrl(StringUtils.trimToEmpty(queuedLink.getUrl()));
		archivedLink.setArchiveDate(DateFormatUtils.format(new Date(),
				"yyyy-MM-dd"));
		archivedLink.setArchiveResult(StringUtils.trimToEmpty(status));
		archivedLink.setArchiveUrl(StringUtils.trimToEmpty(archiveUrl));
		archivedLinkDao.persist(archivedLink);

		return true;
	}

	public void start() {
		new Thread(QueuedLinkProcessor.class.getName()) {
			{
				setDaemon(true);
			}

			@Override
			public void run() {
				QueuedLinkProcessor.this.run();
			};
		}.start();
	}
}
