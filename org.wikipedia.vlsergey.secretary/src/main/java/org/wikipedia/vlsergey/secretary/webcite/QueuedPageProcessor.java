package org.wikipedia.vlsergey.secretary.webcite;

import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
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
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.Text;
import org.wikipedia.vlsergey.secretary.dom.parser.Parser;
import org.wikipedia.vlsergey.secretary.dom.parser.ParsingException;
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

	static boolean ignoreUrl(PerArticleReport perArticleReport, String url) {
		if (!url.startsWith("http://"))
			return true;

		URI uri;
		try {
			uri = URI.create(url);
		} catch (IllegalArgumentException exc) {
			logger.warn("URL " + url + " skipped due wrong format: "
					+ exc.getMessage());

			if (perArticleReport != null)
				perArticleReport.skippedIncorrectFormat(url);

			return true;
		}

		if (StringUtils.isEmpty(uri.getHost())) {
			logger.warn("URL " + url + " skipped due wrong format (no host)");
			return true;
		}

		String host = uri.getHost().toLowerCase();

		return ArticleLinksCollector.isIgnoreHost(perArticleReport, url, host);
	}

	@Autowired
	private ArchivedLinkDao archivedLinkDao;

	@Autowired
	private ArticleLinksCollector articleLinksCollector;

	@Autowired
	private HttpManager httpManager;

	@Autowired
	private LinksQueuer linksQueuer;

	@Autowired
	private MediaWikiBot mediaWikiBot;

	@Autowired
	private QueuedLinkDao queuedLinkDao;

	@Autowired
	private QueuedPageDao queuedPageDao;

	@Autowired
	private WikiCache wikiCache;

	private boolean archive(Long pageId) throws Exception {
		linksQueuer.storeArchivedLinksFromArticle(pageId);

		Revision latestRevision = wikiCache.queryLatestRevision(pageId);
		String latestContent = latestRevision.getContent();
		if (StringUtils.isEmpty(latestContent))
			return true;

		ArticleFragment latestContentDom;
		try {
			latestContentDom = new Parser().parse(latestContent);
		} catch (ParsingException exc) {
			logger.error("Parsing exception occur when parsing page #"
					+ latestRevision.getPage().getId() + " ('"
					+ latestRevision.getPage().getTitle() + "')");
			throw exc;
		}

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

			StringBuilder commentBuilder = new StringBuilder();
			commentBuilder.append(SUMMARY);
			commentBuilder.append(":");
			if (!perArticleReport.archived.isEmpty()) {
				commentBuilder.append(" ");
				commentBuilder.append(perArticleReport.archived.size());
				commentBuilder.append(" archived;");
			}
			if (!perArticleReport.dead.isEmpty()) {
				commentBuilder.append(" ");
				commentBuilder.append(perArticleReport.dead.size());
				commentBuilder.append(" marked dead;");
			}
			String comment = commentBuilder.toString();

			mediaWikiBot.writeContent(latestRevision.getPage(), latestRevision,
					latestContentDom.toString(), comment, true, false);
			writeReport(latestRevision.getPage().getTitle(), perArticleReport);
		}

		return true;
	}

	private boolean archive(PerArticleReport perArticleReport,
			Set<ArticleLink> linksToArchive) throws Exception {
		if (linksToArchive == null || linksToArchive.isEmpty())
			return true;

		boolean hasUncompletedLinks = false;

		for (ArticleLink articleLink : linksToArchive) {
			if (ignoreCite(perArticleReport, articleLink.template))
				continue;

			if (queuedLinkDao.hasLink(articleLink.url, articleLink.accessDate)) {
				logger.debug("Skip link '" + articleLink.url
						+ "' because already queued");
				hasUncompletedLinks = true;
				continue;
			}

			final boolean multilineFormat = articleLink.template.toString()
					.contains("\n");

			ArchivedLink archivedLink = archivedLinkDao.findLink(
					articleLink.url, articleLink.accessDate);
			if (archivedLink != null) {
				processArchivedLink(perArticleReport, articleLink,
						multilineFormat, archivedLink);
				continue;
			}

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

			} catch (ClientProtocolException exc) {

				logger.warn("Potential dead link (unknown error): " + url
						+ " — " + exc, exc);
				perArticleReport.potentiallyDead(url, "Неизвестная ошибка");
				continue;

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

				logger.warn("Dead link: " + url + " — " + exc);
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
				// another try?
				archivedLink = linksQueuer.queueOrGetResult(articleLink);

				if (archivedLink == null) {
					logger.debug("URL '" + url
							+ "' were added to archiving queue");
					hasUncompletedLinks = true;
					continue;
				}

				processArchivedLink(perArticleReport, articleLink,
						multilineFormat, archivedLink);
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
			if (ignoreCite(perArticleReport, link.template))
				continue;

			links.add(link);
		}
		return links;
	}

	boolean ignoreCite(PerArticleReport perArticleReport,
			Template citeWebTemplate) {

		Parameter urlParameter = citeWebTemplate
				.getParameter(WikiConstants.PARAMETER_URL);
		if (urlParameter == null
				|| StringUtils.isEmpty(urlParameter.getValue().toString()
						.trim()))
			return true;
		String url = urlParameter.getValue().toString().trim();

		Parameter deadlinkParameter = citeWebTemplate
				.getParameter(WikiConstants.PARAMETER_DEADLINK);
		if (deadlinkParameter != null
				&& StringUtils.isNotEmpty(deadlinkParameter.getValue()
						.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedDead(url);

			return true;
		}

		Parameter archiveurlParameter = citeWebTemplate
				.getParameter(WikiConstants.PARAMETER_ARCHIVEURL);
		if (archiveurlParameter != null
				&& StringUtils.isNotEmpty(archiveurlParameter.getValue()
						.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedArchived(url);

			return true;
		}

		if (ignoreUrl(perArticleReport, url))
			return true;

		return false;
	}

	private void processArchivedLink(PerArticleReport perArticleReport,
			ArticleLink articleLink, final boolean multilineFormat,
			ArchivedLink archivedLink) {
		logger.debug("URL '" + archivedLink.getAccessUrl()
				+ "' were archived at '" + archivedLink.getArchiveUrl() + "'");

		String archiveUrl = archivedLink.getArchiveUrl();
		String status = archivedLink.getArchiveResult();

		if ("success".equals(status)) {
			perArticleReport.archived(archivedLink.getAccessUrl(), archiveUrl);

			articleLink.template
					.getParameters()
					.getChildren()
					.add(new Parameter(new Text(
							WikiConstants.PARAMETER_ARCHIVEURL), new Text(
							archiveUrl)));
			articleLink.template
					.getParameters()
					.getChildren()
					.add(new Parameter(new Text(
							WikiConstants.PARAMETER_ARCHIVEDATE), new Text(
							archivedLink.getArchiveDate())));
			articleLink.template.format(multilineFormat, multilineFormat);

		} else {
			logger.info("Page was queued but rejected later with status '"
					+ status + "'");

			perArticleReport.nonArchived(archivedLink.getAccessUrl(),
					archiveUrl, status);
		}
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
				mediaWikiBot
						.writeContent(
								"Участник:WebCite Archiver/Statistics",
								null,
								"На момент обновления статистики "
										+ "({{subst:CURRENTTIME}} {{REVISIONDAY}}.{{REVISIONMONTH}}) "
										+ "в очереди находилось '''"
										+ queuedPageDao.findCount()
										+ "''' статей и '''"
										+ queuedLinkDao.findCount()
										+ "''' ссылок", null,
								"Update statistics", true, true, false);

				boolean completed = runImpl();

				logger.debug("Sleeping 60 minutes before another cycle with all queued pages...");
				Thread.sleep(1000 * 60 * 60);

				if (completed)
					return;
			} catch (Exception exc) {
				logger.error("" + exc, exc);

				Thread.sleep(1000 * 60);
			}
		}
	}

	private boolean runImpl() throws Exception {
		List<QueuedPage> queuedPages = queuedPageDao.getPagesFromQueue();
		if (queuedPages == null || queuedPages.isEmpty())
			return true;

		for (QueuedPage queuedPage : queuedPages) {
			boolean complete = false;
			try {
				complete = archive(queuedPage.getId());
			} catch (Exception exc) {
				logger.error("Unable to process page #" + queuedPage.getId()
						+ ": " + exc, exc);
			} finally {
				if (complete) {
					queuedPageDao.removePageFromQueue(queuedPage);
					continue;
				}

				queuedPageDao.addPageToQueue(queuedPage.getId(),
						System.currentTimeMillis());
			}
		}

		return queuedPageDao.getPagesFromQueue().isEmpty();
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
