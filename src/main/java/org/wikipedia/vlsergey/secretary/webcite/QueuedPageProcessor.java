package org.wikipedia.vlsergey.secretary.webcite;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.CircularRedirectException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.message.BasicStatusLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.diff.DiffUtils;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.dom.Text;
import org.wikipedia.vlsergey.secretary.dom.parser.ParsingException;
import org.wikipedia.vlsergey.secretary.http.HttpManager;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.wikipedia.vlsergey.secretary.utils.DateNormalizer;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class QueuedPageProcessor {

	private static final Logger logger = LoggerFactory.getLogger(QueuedPageProcessor.class);

	static boolean ignoreUrl(PerArticleReport perArticleReport, String url) {
		if (!url.startsWith("http://"))
			return true;

		URI uri;
		try {
			uri = URI.create(url);
		} catch (IllegalArgumentException exc) {
			logger.warn("URL " + url + " skipped due wrong format: " + exc.getMessage());

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

	private ArticleLinksCollector articleLinksCollector;

	@Autowired
	private DateNormalizer dateNormalizer;

	@Autowired
	private HttpManager httpManager;

	@Autowired
	private LinksQueuer linksQueuer;

	private Locale locale;

	private MediaWikiBot mediaWikiBot;

	@Autowired
	private QueuedLinkDao queuedLinkDao;

	@Autowired
	private QueuedPageDao queuedPageDao;

	private WebCiteParser webCiteParser;

	private WikiCache wikiCache;

	private WikiConstants wikiConstants;

	private boolean archive(Long pageId, Revision latestRevision, long selfPagePriority) throws Exception {
		// linksQueuer.storeArchivedLinksFromArticle(pageId);

		if (latestRevision == null)
			return true;

		if (latestRevision.getPage().getNamespace().longValue() != 0) {
			if (!StringUtils.startsWithIgnoreCase(latestRevision.getPage().getTitle(), "Википедия:Пресса о Википедии")
					&& !StringUtils.startsWithIgnoreCase(latestRevision.getPage().getTitle(),
							"Вікіпедія:Публікації про Вікіпедію")) {
				logger.info("Skip page #" + latestRevision.getPage().getId() + " ('"
						+ latestRevision.getPage().getTitle() + "') because not an article");
				return true;
			}
		}

		String reportPage = "User:" + mediaWikiBot.getLogin() + "/" + DateFormatUtils.format(new Date(), "yyyyMMddHH");
		String reportAnchor = Long.toString(pageId.longValue(), Character.MAX_RADIX);
		String reportLink = reportPage + "#" + reportAnchor;

		ArticleFragment latestContentDom;
		final String oldContent = latestRevision.getContent();
		try {
			String latestContent = oldContent;
			if (StringUtils.isEmpty(latestContent))
				return true;

			final String xml = latestRevision.getXml();
			if (StringUtils.isEmpty(xml)) {
				throw new Exception("XML not present for page #" + latestRevision.getPage().getId() + " ('"
						+ latestRevision.getPage().getTitle() + "')");
			}

			latestContentDom = getWebCiteParser().parse(xml);

			if (!StringUtils.equals(latestContent, latestContentDom.toWiki(false))) {
				logger.warn("Parsing content not equal to stored content for page #" + latestRevision.getPage().getId()
						+ " ('" + latestRevision.getPage().getTitle() + "')");
			}

		} catch (ParsingException exc) {
			logger.error("Parsing exception occur when parsing page #" + latestRevision.getPage().getId() + " ('"
					+ latestRevision.getPage().getTitle() + "')");

			throw exc;
		}

		final LinkedHashMap<String, List<Template>> templates = latestContentDom.getAllTemplates();
		long pagePriority = selfPagePriority + (templates.containsKey("Избранная статья") ? 100 : 0)
				+ (templates.containsKey("Хорошая статья") ? 100 : 0);

		PerArticleReport perArticleReport = new PerArticleReport();
		List<ArticleLink> linksToProcess = getAllSupportedLinks(perArticleReport, latestContentDom);
		logger.debug("Links to process: " + linksToProcess);

		boolean complete = archive(perArticleReport, linksToProcess, pagePriority);

		if (!complete) {
			return false;
		}

		assert complete;

		final String newContent = latestContentDom.toWiki(false);
		if (!oldContent.equals(latestContentDom.toWiki(false))) {

			if (!perArticleReport.hasChanges()) {
				System.out.println(DiffUtils.getDiff(oldContent, newContent));
				throw new Exception("Article text changed, but no changes in report");
			}

			StringBuilder commentBuilder = new StringBuilder();
			commentBuilder.append("[[");
			commentBuilder.append(reportLink);
			commentBuilder.append("|");
			if (!perArticleReport.archived.isEmpty()) {
				commentBuilder.append(perArticleReport.archived.size());
				commentBuilder.append(" archived;");
			}
			if (!perArticleReport.dead.isEmpty()) {
				commentBuilder.append(" ");
				commentBuilder.append(perArticleReport.dead.size());
				commentBuilder.append(" marked dead;");
			}
			commentBuilder.append("]]");
			String comment = commentBuilder.toString().trim();

			mediaWikiBot.writeContent(latestRevision.getPage(), latestRevision, newContent, comment, true);

			writeReport(reportPage, reportAnchor, latestRevision.getPage().getTitle(), perArticleReport);
		}

		return true;
	}

	private boolean archive(PerArticleReport perArticleReport, List<ArticleLink> linksToArchive, long pagePriority)
			throws Exception {
		if (linksToArchive == null || linksToArchive.isEmpty())
			return true;

		boolean hasUncompletedLinks = false;

		for (ArticleLink articleLink : linksToArchive) {
			if (ignoreCite(perArticleReport, articleLink.template))
				continue;

			if (queuedLinkDao.hasLink(articleLink.url, articleLink.accessDate)) {
				logger.debug("Skip link '" + articleLink.url + "' because already queued");
				hasUncompletedLinks = true;
				continue;
			}

			final boolean multilineFormat = articleLink.template.toString().contains("\n");

			ArchivedLink archivedLink = archivedLinkDao.findNonBrokenLink(articleLink.url, articleLink.accessDate);
			if (archivedLink != null) {
				processArchivedLink(perArticleReport, articleLink, multilineFormat, archivedLink);
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

			if (url.startsWith("http://www.gzt.ru/") || url.startsWith("http://gzt.ru/")) {
				logger.warn("Dead project: " + url);
				perArticleReport.dead(url, "проект закрыт");
				setParameterValue(articleLink.template, wikiConstants.deadlink(), new Text("project-closed"));
				normilizeDates(articleLink.template);
				articleLink.template.format(multilineFormat, multilineFormat);
				continue;
			}

			StatusLine statusLine;
			try {
				HttpGet httpGet = new HttpGet(uri);
				httpManager.setDefaultHttpClientParams(httpGet.getParams());

				statusLine = httpManager.executeFromLocalhost(httpGet, new ResponseHandler<StatusLine>() {
					@Override
					public StatusLine handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
						try {
							return (StatusLine) ((BasicStatusLine) response.getStatusLine()).clone();
						} catch (CloneNotSupportedException e) {
							throw new RuntimeException(e);
						}
					}
				});

			} catch (ClientProtocolException exc) {

				if (exc.getCause() instanceof CircularRedirectException) {
					logger.warn("Dead link: " + url + " — " + exc);
					perArticleReport.dead(url, "циклическая переадресация (circular redirect)");
					setParameterValue(articleLink.template, wikiConstants.deadlink(), new Text("circular-redirect"));
					normilizeDates(articleLink.template);
					articleLink.template.format(multilineFormat, multilineFormat);
					continue;
				}

				logger.warn("Potential dead link (unknown error): " + url + " — " + exc, exc);
				perArticleReport.potentiallyDead(url, "Неизвестная ошибка");
				continue;

			} catch (java.net.ConnectException exc) {

				logger.warn("Potential dead link: " + url + " — " + exc.getMessage());
				perArticleReport.potentiallyDead(url, "отсутствует соединение с сервером (unable to connect)");
				continue;

			} catch (java.net.SocketTimeoutException exc) {

				logger.warn("Potential dead link: " + url + " — " + exc.getMessage());
				perArticleReport.potentiallyDead(url, "истекло время ожидания ответа (timeout)");
				continue;

			} catch (java.net.UnknownHostException exc) {

				logger.warn("Dead link: " + url + " — " + exc);
				perArticleReport.dead(url, "сервер не найден (uknown host)");
				setParameterValue(articleLink.template, wikiConstants.deadlink(), new Text("unknown-host"));
				normilizeDates(articleLink.template);
				articleLink.template.format(multilineFormat, multilineFormat);
				continue;

			}

			logger.debug("Status is " + statusLine);
			int statusCode = statusLine.getStatusCode();
			switch (statusCode) {
			case 200:
				// another try?
				archivedLink = linksQueuer.queueOrGetResult(articleLink, pagePriority);

				if (archivedLink == null) {
					logger.debug("URL '" + url + "' were added to archiving queue");
					hasUncompletedLinks = true;
					continue;
				}

				processArchivedLink(perArticleReport, articleLink, multilineFormat, archivedLink);
				break;
			case 400:
			case 401:
			case 403:
			case 404:
			case 410:

				// broken link

				logger.warn("Dead link: " + url + " — " + statusLine);
				perArticleReport.dead(url, "страница недоступна (" + statusLine + ")");

				setParameterValue(articleLink.template, wikiConstants.deadlink(), new Text("" + statusCode));
				normilizeDates(articleLink.template);
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

	public void clearQueue() {
		queuedPageDao.removeAll(getLocale());
	}

	private List<ArticleLink> getAllSupportedLinks(PerArticleReport perArticleReport, ArticleFragment dom) {
		List<ArticleLink> links = new ArrayList<ArticleLink>();
		for (ArticleLink link : articleLinksCollector.getAllLinks(dom)) {
			if (ignoreCite(perArticleReport, link.template))
				continue;
			links.add(link);
		}
		return links;
	}

	public ArticleLinksCollector getArticleLinksCollector() {
		return articleLinksCollector;
	}

	public Locale getLocale() {
		return locale;
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public WebCiteParser getWebCiteParser() {
		return webCiteParser;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	boolean ignoreCite(PerArticleReport perArticleReport, Template citeWebTemplate) {

		Content urlParameter = wikiConstants.url(citeWebTemplate);
		if (urlParameter == null || StringUtils.isEmpty(urlParameter.toString().trim()))
			return true;
		String url = urlParameter.toString().trim();

		Content deadlinkParameter = wikiConstants.deadlink(citeWebTemplate);
		if (deadlinkParameter != null && StringUtils.isNotEmpty(deadlinkParameter.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedDead(url);

			return true;
		}

		Content archiveurlParameter = WikiConstants.getParameterValue(citeWebTemplate, wikiConstants.archiveUrl());
		if (archiveurlParameter != null && StringUtils.isNotEmpty(archiveurlParameter.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedArchived(url);

			return true;
		}

		if (ignoreUrl(perArticleReport, url))
			return true;

		return false;
	}

	private void normilizeDates(Template citeWebTemplate) {
		normilizeDates(citeWebTemplate, wikiConstants.accessDate());
		normilizeDates(citeWebTemplate, wikiConstants.archiveDate());
		normilizeDates(citeWebTemplate, wikiConstants.date());
	}

	private void normilizeDates(Template citeWebTemplate, final String[] parameterNames) {
		for (String parameterName : parameterNames) {
			for (TemplatePart templatePart : citeWebTemplate.getParameters(parameterName)) {
				if (templatePart.getValue() != null) {
					String nonNormilized = templatePart.getValue().toWiki(true);
					String normilized = dateNormalizer.normalizeDate(nonNormilized);
					if (!StringUtils.equals(nonNormilized, normilized)) {
						templatePart.setValue(new Text(normilized));
					}
				}
			}
		}
	}

	private void processArchivedLink(PerArticleReport perArticleReport, ArticleLink articleLink,
			final boolean multilineFormat, ArchivedLink archivedLink) {
		logger.debug("URL '" + archivedLink.getAccessUrl() + "' were archived at '" + archivedLink.getArchiveUrl()
				+ "'");

		logger.debug("Status of " + archivedLink.getArchiveUrl() + " is '" + archivedLink.getArchiveResult() + "'...");

		if (!StringUtils.equalsIgnoreCase(ArchivedLink.STATUS_SUCCESS, archivedLink.getArchiveResult())) {
			return;
		}

		String archiveUrl = archivedLink.getArchiveUrl();
		String status = archivedLink.getArchiveResult();

		if (ArchivedLink.STATUS_SUCCESS.equals(status)) {
			perArticleReport.archived(archivedLink.getAccessUrl(), archiveUrl);

			setParameterValue(articleLink.template, wikiConstants.archiveUrl(), new Text(archiveUrl));
			setParameterValue(articleLink.template, wikiConstants.archiveDate(),
					new Text(archivedLink.getArchiveDate()));
			normilizeDates(articleLink.template);
			articleLink.template.format(multilineFormat, multilineFormat);

		} else {
			logger.info("Page was queued but rejected later with status '" + status + "'");

			perArticleReport.nonArchived(archivedLink.getAccessUrl(), archiveUrl, status);
		}
	}

	public void run() {
		while (true) {
			try {
				boolean completed = runImpl();

				// long toSleep = queuedPageDao.findCount() / 60 / 10;
				long toSleep = 60;
				logger.debug("Sleeping " + toSleep + " minutes before another cycle with all queued pages...");
				Thread.sleep(1000l * 60l * toSleep);

				if (completed)
					return;
			} catch (Exception exc) {
				logger.error("" + exc, exc);

				try {
					Thread.sleep(1000 * 60);
				} catch (InterruptedException exc2) {
					// ?
				}
			}
		}
	}

	private boolean runImpl() throws Exception {
		List<QueuedPage> queuedPages = queuedPageDao.getPagesFromQueue(getLocale());
		if (queuedPages == null || queuedPages.isEmpty())
			return true;

		Map<Long, QueuedPage> qPages = new LinkedHashMap<Long, QueuedPage>();
		for (QueuedPage queuedPage : queuedPages) {
			qPages.put(queuedPage.getPageId(), queuedPage);
		}
		Iterable<Long> allIds = new ArrayList<Long>(qPages.keySet());

		long lastStatUpdate = 0;
		for (Revision revision : wikiCache.queryLatestContentByPageIdsF().makeBatched(500).apply(allIds)) {
			QueuedPage queuedPage = qPages.get(revision.getPage().getId());
			qPages.remove(revision.getPage().getId());

			if (System.currentTimeMillis() - lastStatUpdate > DateUtils.MILLIS_PER_HOUR) {
				final long pages = queuedPageDao.findCount(getLocale());
				final long links = queuedLinkDao.findCount();
				mediaWikiBot.writeContent("User:" + mediaWikiBot.getLogin() + "/Statistics", null,
						"На момент обновления статистики "
								+ "({{subst:CURRENTTIME}} {{subst:CURRENTMONTHABBREV}}, {{subst:CURRENTDAY2}}) "
								+ "в очереди находилось '''" + pages + "''' страниц и '''" + links + "''' ссылок",
						null, "Update statistics: " + pages + " / " + links, true, false);
				lastStatUpdate = System.currentTimeMillis();
			}

			boolean complete = false;
			try {
				complete = archive(queuedPage.getPageId(), revision, queuedPage.getPriority());
			} catch (Exception exc) {
				logger.error("Unable to process page #" + queuedPage.getKey() + ": " + exc, exc);
			} finally {
				if (complete) {
					queuedPageDao.removePageFromQueue(queuedPage);
					continue;
				}

				queuedPageDao.addPageToQueue(getLocale(), queuedPage.getPageId(), queuedPage.getPriority(),
						System.currentTimeMillis());
			}
		}

		for (QueuedPage queuedPage : qPages.values()) {
			// no last revision
			logger.info("Remove page #" + queuedPage.getKey()
					+ " from queue because no latest revision were found for it");
			queuedPageDao.removePageFromQueue(queuedPage);
		}

		return queuedPageDao.getPagesFromQueue(getLocale()).isEmpty();
	}

	public void setArticleLinksCollector(ArticleLinksCollector articleLinksCollector) {
		this.articleLinksCollector = articleLinksCollector;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
		this.wikiConstants = WikiConstants.get(locale);
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	private void setParameterValue(Template template, String[] parameterNames, Content content) {
		if (parameterNames.length == 0) {
			throw new UnsupportedOperationException("Not supported parameter");
		}

		for (String possibleParameterName : parameterNames) {
			Content oldValue = template.getParameterValue(possibleParameterName);
			if (oldValue != null) {
				template.setParameterValue(possibleParameterName, content);
				return;
			}
		}

		template.setParameterValue(parameterNames[0], content);
	}

	public void setWebCiteParser(WebCiteParser webCiteParser) {
		this.webCiteParser = webCiteParser;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}

	private void writeReport(String reportPage, String anchor, String articleName, PerArticleReport perArticleReport) {
		if (!perArticleReport.hasChanges())
			return;

		StringBuilder commentBuilder = new StringBuilder();
		commentBuilder.append("/* " + articleName + " */ ");
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

		try {
			mediaWikiBot.writeContent(reportPage, null, null,
					perArticleReport.toWiki("[[" + articleName + "]]", anchor, false), comment, false, false);
		} catch (ProcessException exc) {
			// antispam?
			mediaWikiBot.writeContent("Обсуждение:" + articleName, null, null,
					perArticleReport.toWiki("[[" + articleName + "]]", anchor, true), comment, false, false);
		}

	}
}
