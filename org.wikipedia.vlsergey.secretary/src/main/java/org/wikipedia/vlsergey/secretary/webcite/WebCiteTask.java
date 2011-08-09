package org.wikipedia.vlsergey.secretary.webcite;

import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.cache.WikiAccess;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.WikiDOMUtils;
import org.wikipedia.vlsergey.secretary.dom.parser.Parser;

/**
 * Takes article from queue and process WebCitation task with it
 * 
 * @author Sergey
 */
public class WebCiteTask {

	@Autowired
	private WikiAccess wikiAccess;

	public void doTask(String articleName) throws Exception {
		// receive current version to enumerate cite-web templates
		String content = wikiAccess.getLatestRevisionContent(articleName);
		System.out.println(content);
		
		new Parser().parse(content);
	}
}
