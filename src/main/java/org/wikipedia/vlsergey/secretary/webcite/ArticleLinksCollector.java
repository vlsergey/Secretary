package org.wikipedia.vlsergey.secretary.webcite;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.dom.AbstractContainer;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.utils.DateNormalizer;

public class ArticleLinksCollector {

	private static final Logger logger = LoggerFactory.getLogger(ArticleLinksCollector.class);

	static String getParameterOrEmpty(Template template, String[] parameterNames) {
		Content value = null;
		for (String possibleParameterName : parameterNames) {
			value = template.getParameterValue(possibleParameterName);
			if (value != null) {
				break;
			}
		}

		if (value == null)
			return StringUtils.EMPTY;

		return StringUtils.trimToEmpty(value.toWiki(true));
	}

	static boolean isIgnoreHost(PerArticleReport perArticleReport, String url, String host) {

		if (WebCiteArchiver.SKIP_BLACKLISTED.contains(host)) {
			logger.debug("URL " + url + " skipped because some of it's URLs are blacklisted on WebCite");

			if (perArticleReport != null)
				perArticleReport.skippedIgnoreBlacklisted(url);

			return true;
		}

		if (WebCiteArchiver.SKIP_ERRORS.contains(host)) {
			logger.debug("URL " + url + " skipped due to usual errors of WebCite leading to undeadable text");

			if (perArticleReport != null)
				perArticleReport.skippedIgnoreTechLimits(url);

			return true;
		}

		if (WebCiteArchiver.SKIP_TECH_LIMITS.contains(host)) {
			logger.debug("URL " + url + " skipped due to technical limitatios of WebCite");

			if (perArticleReport != null)
				perArticleReport.skippedIgnoreTechLimits(url);

			return true;
		}

		if (WebCiteArchiver.SKIP_NO_CACHE.contains(host)) {
			logger.debug("URL " + url + " skipped because pages on this site usually have 'no-cache' tag");

			if (perArticleReport != null)
				perArticleReport.skippedIgnoreNoCache(url);

			return true;
		}

		if (WebCiteArchiver.SKIP_ARCHIVES.contains(host)) {
			logger.debug("URL " + url + " skipped (are u serious?)");

			if (perArticleReport != null)
				perArticleReport.skippedIgnoreSence(url);

			return true;
		}

		return false;
	}

	@Autowired
	private DateNormalizer dateNormalizer;

	private Locale locale;

	private WikiConstants wikiConstants;

	public List<ArticleLink> getAllLinks(AbstractContainer container) {
		List<ArticleLink> links = new ArrayList<ArticleLink>();
		List<Template> citeWebTemplates = container.getAllTemplates().get(WikiConstants.TEMPLATE_CITE_WEB);

		if (citeWebTemplates == null)
			return Collections.emptyList();

		for (Template citeWebTemplate : citeWebTemplates) {
			ArticleLink articleLink = new ArticleLink();

			articleLink.template = citeWebTemplate;

			articleLink.accessDate = dateNormalizer.normalizeDate(getParameterOrEmpty(citeWebTemplate,
					wikiConstants.accessDate()));
			articleLink.archiveDate = dateNormalizer.normalizeDate(getParameterOrEmpty(citeWebTemplate,
					wikiConstants.archiveDate()));
			articleLink.archiveUrl = getParameterOrEmpty(citeWebTemplate, wikiConstants.archiveUrl());
			articleLink.articleDate = dateNormalizer.normalizeDate(getParameterOrEmpty(citeWebTemplate,
					wikiConstants.date()));
			articleLink.author = getParameterOrEmpty(citeWebTemplate, wikiConstants.deadlink());
			articleLink.title = getParameterOrEmpty(citeWebTemplate, wikiConstants.title());
			articleLink.url = getParameterOrEmpty(citeWebTemplate, wikiConstants.url());

			// strip local refs
			if (StringUtils.contains(articleLink.url, "#"))
				articleLink.url = StringUtils.substringBefore(articleLink.url, "#");

			try {
				URI.create(articleLink.url);
			} catch (IllegalArgumentException exc) {
				logger.warn("URL " + articleLink.url + " skipped due wrong format: " + exc.getMessage());
				continue;
			}

			links.add(articleLink);
		}
		return links;
	}

	public Locale getLocale() {
		return locale;
	}

	public boolean ignoreCite(PerArticleReport perArticleReport, ArticleLink articleLink) {

		if (StringUtils.isEmpty(articleLink.url))
			return true;

		String url = articleLink.url;

		Content deadlinkParameterValue = wikiConstants.deadlink(articleLink.template);
		if (deadlinkParameterValue != null && StringUtils.isNotEmpty(deadlinkParameterValue.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedDead(url);

			return true;
		}

		Content archiveurlParameterValue = wikiConstants.archiveUrl(articleLink.template);
		if (archiveurlParameterValue != null && StringUtils.isNotEmpty(archiveurlParameterValue.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedArchived(url);

			return true;
		}

		return false;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
		this.wikiConstants = WikiConstants.get(locale);
	}
}
