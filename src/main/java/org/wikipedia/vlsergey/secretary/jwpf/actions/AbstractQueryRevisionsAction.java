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
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedRevision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionFlagged;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public abstract class AbstractQueryRevisionsAction extends AbstractQueryAction {

	public static final int MAX_FOR_BOTS = 5000;

	public static final int MAX_FOR_NON_BOTS = 500;

	private List<ParsedPage> pages;

	protected final List<RevisionPropery> properties;

	public Map<String, List<String>> redirectedFrom;

	protected AbstractQueryRevisionsAction(boolean bot, final List<RevisionPropery> properties) {
		super(bot);
		this.properties = properties;
	}

	protected AbstractQueryRevisionsAction(boolean bot, final RevisionPropery[] properties) {
		super(bot);
		this.properties = new ArrayList<RevisionPropery>(Arrays.asList(properties));
	}

	protected int getLimit() {
		return (isBot() ? MAX_FOR_BOTS : MAX_FOR_NON_BOTS);
	}

	public List<ParsedPage> getResults() {
		return pages;
	}

	@Override
	protected ParsedPage parsePage(Element pageElement) throws ProcessException {
		ParsedPage result = super.parsePage(pageElement);

		for (Element child : new ListAdapter<Element>(pageElement.getChildNodes())) {
			parsePageElement(result, child);
		}

		return result;
	}

	protected void parsePageElement(ParsedPage page, Element element) throws ProcessException {
		page.setRevisions(new ArrayList<ParsedRevision>());
		for (Element revElement : new ListAdapter<Element>(element.getElementsByTagName("rev"))) {
			ParsedRevision revisionImpl = parseRevision(page, revElement);
			page.getRevisions().add(revisionImpl);
		}
	}

	@Override
	protected void parseQueryElement(Element queryElement) throws ProcessException {
		redirectedFrom = new LinkedHashMap<String, List<String>>();
		for (Element redirectsElement : new ListAdapter<Element>(queryElement.getElementsByTagName("redirects"))) {
			for (Element rElement : new ListAdapter<Element>(redirectsElement.getElementsByTagName("r"))) {
				final String to = rElement.getAttribute("to");
				final String from = rElement.getAttribute("from");

				List<String> redirects = redirectedFrom.get(to);
				if (redirects == null) {
					redirects = new LinkedList<String>();
					redirectedFrom.put(to, redirects);
				}
				redirects.add(from);
			}
		}

		final ListAdapter<Element> pageElements = new ListAdapter<Element>(queryElement.getElementsByTagName("page"));
		final List<ParsedPage> pages = new ArrayList<ParsedPage>(pageElements.size());
		for (Element pageElement : pageElements) {
			ParsedPage pageImpl = parsePage(pageElement);
			pages.add(pageImpl);

			if (redirectedFrom.containsKey(pageImpl.getTitle())) {
				pageImpl.setRedirectedFrom(redirectedFrom.get(pageImpl.getTitle()));
			}
		}
		this.pages = pages;

	}

	protected ParsedRevision parseRevision(Page page, Element revisionElement) throws ProcessException {
		ParsedRevision revisionImpl = new ParsedRevision(page);

		if (StringUtils.isNotEmpty(revisionElement.getAttribute("parsetree"))) {
			revisionImpl.setXml(revisionElement.getAttribute("parsetree"));
		}

		if (properties.contains(RevisionPropery.COMMENT))
			revisionImpl.setComment(revisionElement.getAttribute("comment"));

		if (properties.contains(RevisionPropery.CONTENT)) {
			revisionImpl.setContent(getText(revisionElement));
		}

		if (properties.contains(RevisionPropery.FLAGS)) {
			revisionImpl.setAnon(revisionElement.hasAttribute("anon"));
			revisionImpl.setBot(revisionElement.hasAttribute("bot"));
			revisionImpl.setMinor(revisionElement.hasAttribute("minor"));
		}

		if (properties.contains(RevisionPropery.IDS)) {
			revisionImpl.setId(new Long(revisionElement.getAttribute("revid")));
		}

		if (properties.contains(RevisionPropery.SIZE) && revisionElement.hasAttribute("size")) {
			revisionImpl.setSize(new Long(revisionElement.getAttribute("size")));
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

		if (properties.contains(RevisionPropery.FLAGGED)) {
			for (Element revElement : new ListAdapter<Element>(revisionElement.getElementsByTagName("flagged"))) {
				RevisionFlagged revisionFlagged = parseRevisionFlagged(revisionImpl, revElement);

				if (revisionImpl.getFlagged() == null) {
					revisionImpl.setFlagged(new ArrayList<RevisionFlagged>(1));
				}
				revisionImpl.getFlagged().add(revisionFlagged);
			}
		}

		return revisionImpl;
	}

	protected RevisionFlagged parseRevisionFlagged(ParsedRevision revision, Element flaggedElement) {
		/*
		 * <flagged user="Klip game" timestamp="2012-05-04T17:01:13Z" level="0"
		 * level_text="stable">
		 * 
		 * <tags accuracy="1" />
		 * 
		 * </flagged>
		 */
		RevisionFlagged revisionFlagged = new RevisionFlagged();

		if (flaggedElement.hasAttribute("user")) {
			revisionFlagged.setUser(flaggedElement.getAttribute("user"));
		}

		if (flaggedElement.hasAttribute("timestamp")) {
			try {
				String timestamp = flaggedElement.getAttribute("timestamp");
				Date date = parseDate(timestamp);
				revisionFlagged.setTimestamp(date);
			} catch (ParseException exc) {
				throw new ProcessException(exc.getMessage(), exc);
			}
		}

		if (flaggedElement.hasAttribute("level")) {
			try {
				String level = flaggedElement.getAttribute("level");
				Integer integer = Integer.valueOf(level);
				revisionFlagged.setLevel(integer);
			} catch (NumberFormatException exc) {
				throw new ProcessException(exc.getMessage(), exc);
			}
		}

		if (flaggedElement.hasAttribute("level_text")) {
			revisionFlagged.setLevelText(flaggedElement.getAttribute("level_text"));
		}

		List<Integer> tagsAccuracy = new ArrayList<Integer>(1);
		for (Element tagsElement : new ListAdapter<Element>(flaggedElement.getElementsByTagName("tags"))) {
			if (tagsElement.hasAttribute("accuracy")) {
				try {
					String accuracy = tagsElement.getAttribute("accuracy");
					Integer integer = Integer.valueOf(accuracy);
					tagsAccuracy.add(integer);
				} catch (NumberFormatException exc) {
					throw new ProcessException(exc.getMessage(), exc);
				}
			}
		}
		revisionFlagged.setTagsAccuracy(tagsAccuracy);

		return revisionFlagged;
	}

	@Override
	public void reset() {
		super.reset();
		this.pages = null;
		this.redirectedFrom = null;
	}

}