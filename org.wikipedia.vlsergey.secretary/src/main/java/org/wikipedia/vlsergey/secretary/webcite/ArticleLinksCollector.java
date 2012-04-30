package org.wikipedia.vlsergey.secretary.webcite;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikipedia.vlsergey.secretary.dom.AbstractContainer;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;

public class ArticleLinksCollector {

	private static final Logger logger = LoggerFactory.getLogger(ArticleLinksCollector.class);

	static String getParameterOrEmpty(Template template, String parameterName) {
		final Content value = template.getParameterValue(parameterName);

		if (value == null)
			return StringUtils.EMPTY;

		return StringUtils.trimToEmpty(value.toWiki());
	}

	static boolean isIgnoreHost(PerArticleReport perArticleReport, String url, String host) {

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

	public List<ArticleLink> getAllLinks(AbstractContainer container) {
		List<ArticleLink> links = new ArrayList<ArticleLink>();
		List<Template> citeWebTemplates = container.getAllTemplates().get(WikiConstants.TEMPLATE_CITE_WEB);

		if (citeWebTemplates == null)
			return Collections.emptyList();

		for (Template citeWebTemplate : citeWebTemplates) {
			ArticleLink articleLink = new ArticleLink();

			articleLink.template = citeWebTemplate;

			articleLink.accessDate = getParameterOrEmpty(citeWebTemplate, WikiConstants.PARAMETER_ACCESSDATE);
			articleLink.archiveDate = getParameterOrEmpty(citeWebTemplate, WikiConstants.PARAMETER_ARCHIVEDATE);
			articleLink.archiveUrl = getParameterOrEmpty(citeWebTemplate, WikiConstants.PARAMETER_ARCHIVEURL);
			articleLink.articleDate = getParameterOrEmpty(citeWebTemplate, WikiConstants.PARAMETER_DATE);
			articleLink.author = getParameterOrEmpty(citeWebTemplate, WikiConstants.PARAMETER_DEADLINK);
			articleLink.title = getParameterOrEmpty(citeWebTemplate, WikiConstants.PARAMETER_TITLE);
			articleLink.url = getParameterOrEmpty(citeWebTemplate, WikiConstants.PARAMETER_URL);

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

	public boolean ignoreCite(PerArticleReport perArticleReport, ArticleLink articleLink) {

		if (StringUtils.isEmpty(articleLink.url))
			return true;

		String url = articleLink.url;

		Content deadlinkParameterValue = articleLink.template.getParameterValue(WikiConstants.PARAMETER_DEADLINK);
		if (deadlinkParameterValue != null && StringUtils.isNotEmpty(deadlinkParameterValue.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedDead(url);

			return true;
		}

		Content archiveurlParameterValue = articleLink.template.getParameterValue(WikiConstants.PARAMETER_ARCHIVEURL);
		if (archiveurlParameterValue != null && StringUtils.isNotEmpty(archiveurlParameterValue.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedArchived(url);

			return true;
		}

		return false;
	}
}
