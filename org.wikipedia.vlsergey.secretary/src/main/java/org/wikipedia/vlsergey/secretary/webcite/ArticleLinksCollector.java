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
import org.wikipedia.vlsergey.secretary.dom.Parameter;
import org.wikipedia.vlsergey.secretary.dom.Template;

public class ArticleLinksCollector {

	private static final Logger logger = LoggerFactory
			.getLogger(ArticleLinksCollector.class);

	public List<ArticleLink> getAllLinks(AbstractContainer container) {
		List<ArticleLink> links = new ArrayList<ArticleLink>();
		List<Template> citeWebTemplates = container.getAllTemplates().get(
				WikiConstants.TEMPLATE_CITE_WEB);

		if (citeWebTemplates == null)
			return Collections.emptyList();

		for (Template citeWebTemplate : citeWebTemplates) {
			ArticleLink articleLink = new ArticleLink();

			articleLink.template = citeWebTemplate;

			articleLink.accessDate = getParameterOrEmpty(citeWebTemplate,
					WikiConstants.PARAMETER_ACCESSDATE);
			articleLink.archiveDate = getParameterOrEmpty(citeWebTemplate,
					WikiConstants.PARAMETER_ARCHIVEDATE);
			articleLink.archiveUrl = getParameterOrEmpty(citeWebTemplate,
					WikiConstants.PARAMETER_ARCHIVEURL);
			articleLink.articleDate = getParameterOrEmpty(citeWebTemplate,
					WikiConstants.PARAMETER_DATE);
			articleLink.author = getParameterOrEmpty(citeWebTemplate,
					WikiConstants.PARAMETER_DEADLINK);
			articleLink.title = getParameterOrEmpty(citeWebTemplate,
					WikiConstants.PARAMETER_TITLE);
			articleLink.url = getParameterOrEmpty(citeWebTemplate,
					WikiConstants.PARAMETER_URL);

			links.add(articleLink);
		}
		return links;
	}

	static String getParameterOrEmpty(Template template, String parameterName) {
		Parameter parameter = template.getParameter(parameterName);
		if (parameter == null)
			return StringUtils.EMPTY;

		final Content value = parameter.getValue();

		if (value == null)
			return StringUtils.EMPTY;

		return StringUtils.trimToEmpty(value.toWiki());
	}

	public boolean ignoreCite(PerArticleReport perArticleReport,
			ArticleLink articleLink) {

		if (StringUtils.isEmpty(articleLink.url))
			return true;

		String url = articleLink.url;

		Parameter deadlinkParameter = articleLink.template
				.getParameter(WikiConstants.PARAMETER_DEADLINK);
		if (deadlinkParameter != null
				&& StringUtils.isNotEmpty(deadlinkParameter.getValue()
						.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedDead(url);

			return true;
		}

		Parameter archiveurlParameter = articleLink.template
				.getParameter(WikiConstants.PARAMETER_ARCHIVEURL);
		if (archiveurlParameter != null
				&& StringUtils.isNotEmpty(archiveurlParameter.getValue()
						.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedArchived(url);

			return true;
		}

		return false;
	}

	public boolean ignoreUrl(PerArticleReport perArticleReport, String url) {
		if (!url.startsWith("http://"))
			return true;

		URI uri;
		try {
			uri = URI.create(url);
		} catch (IllegalArgumentException exc) {
			logger.warn("URL " + url + " skipped due wrong format: "
					+ exc.getMessage());

			return true;
		}

		String host = uri.getHost().toLowerCase();
		return WebCiteArchiver.isIgnoreHost(null, url, host);
	}

}
