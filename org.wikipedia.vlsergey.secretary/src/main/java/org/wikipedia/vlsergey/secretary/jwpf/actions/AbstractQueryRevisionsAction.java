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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPageImpl;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedRevisionImpl;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public abstract class AbstractQueryRevisionsAction extends AbstractQueryAction {

	private final List<RevisionPropery> properties;

	private List<Page> result;

	protected AbstractQueryRevisionsAction(
			final List<RevisionPropery> properties) {
		super();
		this.properties = properties;
	}

	protected AbstractQueryRevisionsAction(final RevisionPropery[] properties) {
		super();
		this.properties = new ArrayList<RevisionPropery>(
				Arrays.asList(properties));
	}

	@Override
	public int getLimit() {
		return super.getLimit() / 10;
	}

	public List<Page> getResults() {
		return result;
	}

	@Override
	protected ParsedPageImpl parsePage(Element pageElement)
			throws ProcessException {
		ParsedPageImpl result = super.parsePage(pageElement);

		for (Element child : new ListAdapter<Element>(
				pageElement.getChildNodes())) {
			parsePageElement(result, child);
		}

		return result;
	}

	protected void parsePageElement(ParsedPageImpl page, Element element)
			throws ProcessException {
		page.setRevisions(new ArrayList<Revision>());
		for (Element revElement : new ListAdapter<Element>(
				element.getElementsByTagName("rev"))) {
			ParsedRevisionImpl revisionImpl = parseRevision(page, revElement);
			page.getRevisions().add(revisionImpl);
		}
	}

	@Override
	protected void parseQueryElement(Element queryElement)
			throws ProcessException {
		final ListAdapter<Element> pageElements = new ListAdapter<Element>(
				queryElement.getElementsByTagName("page"));
		final List<Page> result = new ArrayList<Page>(pageElements.size());

		for (Element pageElement : pageElements) {
			ParsedPageImpl pageImpl = parsePage(pageElement);
			result.add(pageImpl);
		}

		this.result = result;
	}

	protected ParsedRevisionImpl parseRevision(Page page,
			Element revisionElement) throws ProcessException {
		ParsedRevisionImpl revisionImpl = new ParsedRevisionImpl(page);

		if (StringUtils.isNotEmpty(revisionElement.getAttribute("parsetree"))) {
			revisionImpl
					.setParsetree(revisionElement.getAttribute("parsetree"));
		}

		if (properties.contains(RevisionPropery.COMMENT))
			revisionImpl.setComment(revisionElement.getAttribute("comment"));

		if (properties.contains(RevisionPropery.CONTENT)) {
			StringBuilder content = new StringBuilder();
			for (Node child : new ListAdapter<Node>(
					revisionElement.getChildNodes())) {
				if (child instanceof Text) {
					Text text = (Text) child;
					content.append(text.getTextContent());
				}
			}
			revisionImpl.setContent(content.toString());
		}

		if (properties.contains(RevisionPropery.FLAGS)) {
			revisionImpl.setAnon(revisionElement.hasAttribute("anon"));
			revisionImpl.setBot(revisionElement.hasAttribute("bot"));
			revisionImpl.setMinor(revisionElement.hasAttribute("minor"));
		}

		if (properties.contains(RevisionPropery.IDS)) {
			revisionImpl.setId(new Long(revisionElement.getAttribute("revid")));
		}

		if (properties.contains(RevisionPropery.SIZE)
				&& revisionElement.hasAttribute("size")) {
			revisionImpl
					.setSize(new Long(revisionElement.getAttribute("size")));
		}

		if (properties.contains(RevisionPropery.TIMESTAMP)) {
			try {
				String timestamp = revisionElement.getAttribute("timestamp");
				Date date = parseDate(timestamp);
				revisionImpl.setTimestamp(date);
			} catch (ParseException exc) {
				throw new ProcessException(exc.getMessage(), exc);
			}
		}

		if (properties.contains(RevisionPropery.USER)) {
			revisionImpl.setUser(revisionElement.getAttribute("user"));
		}

		return revisionImpl;
	}

}