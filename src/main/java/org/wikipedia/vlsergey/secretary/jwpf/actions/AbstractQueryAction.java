/*
 * Copyright 2001-2008 Fizteh-Center Lab., MIPT, Russia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created on 30.03.2008
 */
package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.text.ParseException;
import java.util.Optional;
import java.util.stream.IntStream;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public abstract class AbstractQueryAction extends AbstractApiXmlAction {

	public AbstractQueryAction(boolean bot) {
		super(bot);
	}

	@Override
	protected void parseAPI(final Element root) throws ProcessException, ParseException {
		final ListAdapter<Element> queryContinueElements = new ListAdapter<Element>(
				root.getElementsByTagName("query-continue"));
		if (queryContinueElements.size() > 1)
			throw new IllegalArgumentException("Too many query-continue elements: " + queryContinueElements.size());
		else if (queryContinueElements.size() == 1)
			parseQueryContinue(queryContinueElements.get(0));

		final NodeList childNodes = root.getChildNodes();
		Optional<Element> queryElement = IntStream.range(0, childNodes.getLength()).mapToObj(childNodes::item)
				.filter(x -> "query".equals(x.getNodeName())).map(x -> (Element) x).findAny();

		if (!queryElement.isPresent()) {
			throw new IllegalArgumentException("No query elements");
		}
		parseQueryElement(queryElement.get());
	}

	protected ParsedPage parsePage(Element pageElement) throws ProcessException {
		ParsedPage pageImpl = new ParsedPage();

		pageImpl.setMissing(pageElement.hasAttribute("missing"));

		if (pageElement.hasAttribute("ns"))
			pageImpl.setNamespace(new Integer(pageElement.getAttribute("ns")));

		if (pageElement.hasAttribute("pageid"))
			pageImpl.setId(new Long(pageElement.getAttribute("pageid")));

		if (pageElement.hasAttribute("title"))
			pageImpl.setTitle(pageElement.getAttribute("title"));

		return pageImpl;
	}

	protected void parseQueryContinue(Element queryContinueElement) throws ParseException {
		throw new UnsupportedOperationException("This is not multiaction operation: " + this.getClass().getName());
	}

	protected abstract void parseQueryElement(Element queryElement) throws ProcessException, ParseException;

}