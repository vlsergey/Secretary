package org.wikipedia.vlsergey.secretary.webcite;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicStatusLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Parameter;
import org.wikipedia.vlsergey.secretary.dom.Text;
import org.wikipedia.vlsergey.secretary.dom.parser.Parser;
import org.wikipedia.vlsergey.secretary.http.HttpManager;
import org.wikipedia.vlsergey.secretary.jwpf.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class QueuedPageProcessor {
	private static final Logger logger = LoggerFactory
			.getLogger(QueuedPageProcessor.class);

	private static final String SUMMARY = "Archiving links in WebCite";

	private static final boolean WAIT_MONTH = false;

	@Autowired
	private ArticleLinksCollector articleLinksCollector;

	@Autowired
	private HttpManager httpManager;

	@Autowired
	private LinksQueuer linksQueuer;

	@Autowired
	private MediaWikiBot mediaWikiBot;

	@Autowired
	private QueuedPageDao queuedPageDao;

	@Autowired
	private WebCiteArchiver webCiteArchiver;

	@Autowired
	private WikiCache wikiCache;

	private boolean archive(Long pageId) throws Exception {
		linksQueuer.storeArchivedLinksFromArticle(pageId);

		Revision latestRevision = wikiCache.queryLatestRevision(pageId);
		String latestContent = latestRevision.getContent();
		if (StringUtils.isEmpty(latestContent))
			return true;

		ArticleFragment latestContentDom = new Parser().parse(latestContent);

		PerArticleReport perArticleReport = new PerArticleReport();
		Set<ArticleLink> latestLinks = getAllSupportedLinks(perArticleReport,
				latestContentDom);

		final Set<ArticleLink> linksToProcess;
		if (WAIT_MONTH) {
			logger.debug("Links in latest version of article: " + latestLinks);

			String monthlyRevision = queryPageContentOlderThanNDays(
					latestRevision, 30);
			ArticleFragment monthlyContent = new Parser()
					.parse(monthlyRevision);
			Set<ArticleLink> monthlyLinks = getAllSupportedLinks(null,
					monthlyContent);
			logger.debug("Links in monthly version of article: " + monthlyLinks);

			Set<ArticleLink> newLinks = new HashSet<ArticleLink>(latestLinks);
			newLinks.removeAll(monthlyLinks);
			linksToProcess = new HashSet<ArticleLink>(latestLinks);
			linksToProcess.removeAll(newLinks);
		} else {
			linksToProcess = latestLinks;
		}
		logger.debug("Links to process: " + linksToProcess);

		boolean complete = archive(perArticleReport, linksToProcess);

		if (!complete) {
			return false;
		}

		assert complete;

		if (!latestRevision.getContent().equals(latestContentDom.toString())) {
			mediaWikiBot.writeContent(latestRevision.getPage(), latestRevision,
					latestContentDom.toString(), SUMMARY + " ("
							+ perArticleReport.archived.size()
							+ " link(s) archived)", true, false);
		}

		writeReport(latestRevision.getPage().getTitle(), perArticleReport);
		return true;
	}

	private boolean archive(PerArticleReport perArticleReport,
			Set<ArticleLink> linksToArchive) throws Exception {
		String archivedDate = DateFormatUtils.format(new Date(), "yyyy-MM-dd");

		if (linksToArchive == null || linksToArchive.isEmpty())
			return true;

		boolean hasUncompletedLinks = false;

		for (ArticleLink articleLink : linksToArchive) {
			if (webCiteArchiver.ignoreCite(perArticleReport,
					articleLink.template))
				continue;

			final boolean multilineFormat = articleLink.template.toString()
					.contains("\n");
			final String url = articleLink.url;

			// do not check with "#"
			URI uri;
			if (StringUtils.contains(url, "#")) {
				uri = URI.create(StringUtils.substringBefore(url, "#"));
			} else {
				uri = URI.create(url);
			}

			logger.debug("Checking status of " + url);

			StatusLine statusLine;
			try {
				HttpGet httpGet = new HttpGet(uri);
				httpGet.getParams().setParameter("http.socket.timeout",
						new Integer((int) DateUtils.MILLIS_PER_MINUTE));

				statusLine = httpManager.execute(httpGet,
						new ResponseHandler<StatusLine>() {
							public StatusLine handleResponse(
									HttpResponse response)
									throws ClientProtocolException, IOException {
								try {
									return (StatusLine) ((BasicStatusLine) response
											.getStatusLine()).clone();
								} catch (CloneNotSupportedException e) {
									throw new RuntimeException(e);
								}
							}
						});

			} catch (java.net.ConnectException exc) {
				logger.warn("Potential dead link: " + url + " — "
						+ exc.getMessage());

				perArticleReport
						.potentiallyDead(url,
								"отсутствует соединение с сервером (unable to connect)");

				continue;

			} catch (java.net.SocketTimeoutException exc) {
				logger.warn("Potential dead link: " + url + " — "
						+ exc.getMessage());

				perArticleReport.potentiallyDead(url,
						"истекло время ожидания ответа (timeout)");

				continue;

			} catch (java.net.UnknownHostException exc) {

				logger.warn("Dead link: " + url + " — " + exc.getMessage());

				perArticleReport.dead(url, "сервер не найден (uknown host)");

				articleLink.template
						.getParameters()
						.getChildren()
						.add(new Parameter(new Text(
								WikiConstants.PARAMETER_DEADLINK), new Text(
								"unknown-host")));
				articleLink.template.format(multilineFormat, multilineFormat);
				continue;
			}

			logger.debug("Status is " + statusLine);
			int statusCode = statusLine.getStatusCode();
			switch (statusCode) {
			case 200:
				// okay, archiving
				ArchivedLink archivedLink = linksQueuer
						.queueOrGetResult(articleLink);

				if (archivedLink == null) {
					logger.debug("URL '" + url
							+ "' were added to archiving queue");
					hasUncompletedLinks = true;
					continue;
				}

				logger.debug("URL '" + url + "' were archived at '"
						+ archivedLink.getArchiveUrl() + "'");

				String archiveUrl = archivedLink.getArchiveUrl();
				String status = archivedLink.getArchiveResult();

				if ("success".equals(status)) {
					perArticleReport.archived(url, archiveUrl);

					articleLink.template
							.getParameters()
							.getChildren()
							.add(new Parameter(new Text(
									WikiConstants.PARAMETER_ARCHIVEURL),
									new Text(archiveUrl)));
					articleLink.template
							.getParameters()
							.getChildren()
							.add(new Parameter(new Text(
									WikiConstants.PARAMETER_ARCHIVEDATE),
									new Text(archivedDate)));
					articleLink.template.format(multilineFormat,
							multilineFormat);

				} else {
					logger.info("Page was queued but rejected later with status '"
							+ status + "'");

					perArticleReport.nonArchived(url, archiveUrl, status);
				}

				break;
			case 401:
			case 403:
			case 404:
			case 410:

				// broken link

				logger.warn("Dead link: " + url + " — " + statusLine);
				perArticleReport.dead(url, "страница недоступна (" + statusLine
						+ ")");

				articleLink.template
						.getParameters()
						.getChildren()
						.add(new Parameter(new Text(
								WikiConstants.PARAMETER_DEADLINK), new Text(""
								+ statusCode)));
				articleLink.template.format(multilineFormat, multilineFormat);

				break;

			default:
				logger.warn("Unsupported status: " + statusLine);
				perArticleReport.skipped(url, "" + statusLine);
				break;
			}
		}

		return !hasUncompletedLinks;
	}

	private Set<ArticleLink> getAllSupportedLinks(
			PerArticleReport perArticleReport, ArticleFragment dom) {
		Set<ArticleLink> links = new LinkedHashSet<ArticleLink>();

		List<ArticleLink> allLinks = articleLinksCollector.getAllLinks(dom);
		for (ArticleLink link : allLinks) {
			if (webCiteArchiver.ignoreCite(perArticleReport, link.template))
				continue;

			links.add(link);
		}
		return links;
	}

	private String queryPageContentOlderThanNDays(Revision latestRevision,
			int days) throws Exception {
		for (Revision revision : mediaWikiBot
				.queryRevisionsByPageId(latestRevision.getPage().getId(),
						latestRevision.getId(), Direction.OLDER,
						RevisionPropery.IDS, RevisionPropery.TIMESTAMP)) {

			if ((System.currentTimeMillis() - revision.getTimestamp().getTime()) >= DateUtils.MILLIS_PER_DAY
					* days) {
				String content = wikiCache.queryRevisionContent(revision
						.getId());

				if (StringUtils.isNotEmpty(content))
					return content;
			}
		}

		return null;
	}

	public void run() throws InterruptedException {
		while (true) {
			try {
				boolean didAnyWork = runImpl();

				Thread.sleep(1000 * 60);

				if (!didAnyWork)
					return;
			} catch (Exception exc) {
				logger.error("" + exc, exc);

				Thread.sleep(1000 * 60);
			}
		}
	}

	private boolean runImpl() throws Exception {
		QueuedPage queuedPage = queuedPageDao.getPageFromQueue();
		if (queuedPage == null)
			return false;

		boolean complete = false;
		try {
			complete = archive(queuedPage.getId());
		} finally {
			if (complete) {
				queuedPageDao.removePageFromQueue(queuedPage);
				return true;
			}

			queuedPageDao.addPageToQueue(queuedPage.getId(),
					System.currentTimeMillis());
		}

		return true;
	}

	private void writeReport(String articleName,
			PerArticleReport perArticleReport) {
		if (!perArticleReport.hasChanges())
			return;

		try {
			mediaWikiBot.writeContent("Обсуждение:" + articleName, null, null,
					perArticleReport.toWiki(false),
					"/* Отчёт бота WebCite Archiver */ WebCite Report", false,
					false, false);
		} catch (ProcessException exc) {
			// antispam?
			mediaWikiBot.writeContent("Обсуждение:" + articleName, null, null,
					perArticleReport.toWiki(true),
					"/* Отчёт бота WebCite Archiver */ WebCite Report", false,
					false, false);
		}

	}
}
