package org.wikipedia.vlsergey.secretary.webcite;

import java.util.Date;
import java.util.List;

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
			} catch (Exception exc) {
				logger.error("" + exc, exc);
			}
		}
	}

	private boolean runImpl() throws Exception {
		QueuedLink queuedLink = queuedLinkDao.getLinkFromQueue();
		if (queuedLink == null)
			return false;

		// are we sure there is no match?
		List<ArchivedLink> archivedLinks = archivedLinkDao.getArchivedLinks(
				queuedLink.getUrl(), queuedLink.getAccessDate());
		if (archivedLinks.size() > 2) {
			logger.error("More than one URLs with same access date ("
					+ queuedLink.getAccessDate() + ") and URL '"
					+ queuedLink.getUrl() + "'");
			System.exit(1);
		}

		if (!archivedLinks.isEmpty()) {
			// already processed
			queuedLinkDao.removeLinkFromQueue(queuedLink);
			return true;
		}

		long nextAllowedTime = webCiteLimiter.getNextAllowedTime();
		while (nextAllowedTime > System.currentTimeMillis()) {
			try {
				logger.debug("Sleeping until " + new Date(nextAllowedTime)
						+ "...");
				final long toSleep = nextAllowedTime
						- System.currentTimeMillis();
				if (toSleep > 0)
					Thread.sleep(toSleep + 100);
			} catch (InterruptedException exc) {
				break;
			}
		}

		// should be allowed now
		webCiteLimiter.beforeRequest();
		String archiveUrl = webCiteArchiver.archive(queuedLink.getUrl(),
				queuedLink.getTitle(), queuedLink.getAuthor(),
				queuedLink.getArticleDate());

		String webCiteCode = StringUtils.substringAfterLast(archiveUrl, "/");
		logger.debug("WebCite code is '" + webCiteCode + "'");

		String status = webCiteArchiver.getStatus(webCiteCode);

		ArchivedLink archivedLink = new ArchivedLink();
		archivedLink.setAccessDate(queuedLink.getAccessDate());
		archivedLink.setAccessUrl(queuedLink.getUrl());
		archivedLink.setArchiveDate(DateFormatUtils.format(new Date(),
				"yyyy-MM-dd"));
		archivedLink.setArchiveResult(status);
		archivedLink.setArchiveUrl(archiveUrl);
		archivedLinkDao.persist(archivedLink);

		return true;
	}

	public void start() {
		new Thread(QueuedLinkProcessor.class.getName()) {
			{
				setDaemon(true);
			}

			public void run() {
				QueuedLinkProcessor.this.run();
			};
		}.start();
	}
}
