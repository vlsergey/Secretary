/*
 * Copyright 2007 Tobias Knerr.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Contributors:
 * Tobias Knerr
 * 
 */
package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.http.client.methods.HttpGet;
import org.w3c.dom.Element;

/**
 * action class using the MediaWiki-api's "list=embeddedin" that is used to find
 * all articles which use a template.
 * 
 * @author Tobias Knerr
 * @since MediaWiki 1.9.0
 */
public class QueryEmbeddedinTitles extends AbstractQueryAction implements
		MultiAction<String> {

	/**
	 * information necessary to get the next api page.
	 */
	private String nextPageInfo = null;

	/**
	 * Collection that will contain the result (titles of articles using the
	 * template) after performing the action has finished.
	 */
	private Collection<String> titleCollection = new ArrayList<String>();

	private String title;

	private String namespaces;

	/**
	 * The private constructor, which is used to create follow-up actions.
	 */
	private QueryEmbeddedinTitles(String title, String namespaces,
			String nextPageInfo) {
		this.title = title;
		this.namespaces = namespaces;

		String query = "/api.php?action=query&list=embeddedin" + "&eititle="
				+ encode(title)
				+ ((namespaces != null) ? ("&einamespace=" + namespaces) : "")
				+ "&eilimit=" + getLimit() + "&format=xml" + "&eicontinue="
				+ encode(nextPageInfo);

		msgs.add(new HttpGet(query));
	}

	/**
	 * The public constructor. It will have an MediaWiki-request generated,
	 * which is then added to msgs. When it is answered, the method
	 * processAllReturningText will be called (from outside this class). For the
	 * parameters, see
	 * {@link QueryEmbeddedinTitles#generateRequest(String, String, String)}
	 */
	public QueryEmbeddedinTitles(String title, String namespaces) {
		this.title = title;
		this.namespaces = namespaces;

		String query = "/api.php?action=query&list=embeddedin" + "&eititle="
				+ encode(title)
				+ ((namespaces != null) ? ("&einamespace=" + namespaces) : "")
				+ "&eilimit=" + getLimit() + "&format=xml";

		msgs.add(new HttpGet(query));
	}

	/**
	 * @return necessary information for the next action or null if no next api
	 *         page exists
	 */
	public QueryEmbeddedinTitles getNextAction() {
		if (nextPageInfo == null) {
			return null;
		} else {
			return new QueryEmbeddedinTitles(title, namespaces, nextPageInfo);
		}
	}

	/**
	 * @return the collected article names
	 */
	public Collection<String> getResults() {
		return titleCollection;
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element categorymembersElement = (Element) queryContinueElement
				.getElementsByTagName("embeddedin").item(0);
		nextPageInfo = categorymembersElement.getAttribute("eicontinue");
	}

	@Override
	protected void parseQueryElement(Element queryElement) {
		for (Element eiElement : new ListAdapter<Element>(
				queryElement.getElementsByTagName("ei"))) {
			titleCollection.add(eiElement.getAttribute("title"));
		}
	}
}
