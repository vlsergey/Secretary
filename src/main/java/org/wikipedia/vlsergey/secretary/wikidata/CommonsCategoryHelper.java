package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiSnak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;

@Component
public class CommonsCategoryHelper extends AbstractHelper {

	@Autowired
	@Qualifier("commonsCache")
	private WikiCache commonsCache;

	public List<ApiSnak> parse(EntityId property, String strValue) {

		try {
			if (strValue.startsWith("Category:") || strValue.startsWith("category:")) {
				String categoryName = strValue.substring("Category:".length()).replace('_', ' ').trim();
				categoryName = Character.toUpperCase(categoryName.charAt(0)) + categoryName.substring(1);

				// check if soft redirect
				Revision commonsCategoryRevision = commonsCache.queryLatestRevision("Category:" + categoryName);
				if (commonsCategoryRevision != null && StringUtils.isNotBlank(commonsCategoryRevision.getXml())) {
					ArticleFragment commonsFragment = commonsCache.getMediaWikiBot().getXmlParser()
							.parse(commonsCategoryRevision);
					for (Template softRedirectTemplate : commonsFragment.getTemplates("category redirect")) {
						TemplatePart templatePart = softRedirectTemplate.getParameter(0);
						if (templatePart != null && templatePart.getName() == null && templatePart.getValue() != null) {
							categoryName = templatePart.getValue().toWiki(true).trim();
						}
					}
				} else if (commonsCategoryRevision == null) {
					throw new UnsupportedParameterValue(categoryName + " (no such category)");
				}

				return Collections.singletonList(ApiSnak.newSnak(property, categoryName));
			}
		} catch (UnsupportedParameterValue exc) {
			throw exc;
		} catch (Exception exc) {
			exc.printStackTrace();
		}

		throw new UnsupportedParameterValue(strValue);

	}

}
