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
 * Created on 10.04.2008
 */
package org.wikipedia.vlsergey.secretary.books;

import gnu.trove.set.TLongSet;
import gnu.trove.set.hash.TLongHashSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Component
public class CountBooks implements Runnable {

	private static final Log log = LogFactory.getLog(CountBooks.class);

	private MediaWikiBot mediaWikiBot;

	private WikiCache wikiCache;

	private void addAuthor(Map<String, TLongSet> byAuthor, Page page, String title, Content parameter) {
		if (parameter == null)
			return;

		String author = StringUtils.trimToEmpty(parameter.toWiki(true));

		author = StringUtils.trimToNull(author);

		if (author == null)
			return;

		if (byAuthor.containsKey(author)) {
			byAuthor.get(author).add(page.getId());
		} else {
			TLongSet set = new TLongHashSet();
			set.add(page.getId());
			byAuthor.put(author, set);
		}
	}

	private void addBookTitle(Map<String, TLongSet> byTitle, Page page, String pageTitle, Content parameter) {
		if (parameter == null)
			return;

		String bookTitle = StringUtils.trimToEmpty(parameter.toWiki(true));

		bookTitle = StringUtils.trimToNull(bookTitle);

		if (bookTitle == null)
			return;

		if (byTitle.containsKey(bookTitle)) {
			byTitle.get(bookTitle).add(page.getId());
		} else {
			TLongSet set = new TLongHashSet();
			set.add(page.getId());
			byTitle.put(bookTitle, set);
		}
	}

	private void addISBN(Map<String, TLongSet> byISBN, Page page, String title, Content parameter) {
		if (parameter == null)
			return;

		String isbn = StringUtils.trimToEmpty(parameter.toWiki(true));

		if (isbn.toLowerCase().startsWith("isbn"))
			isbn = StringUtils.trimToEmpty(isbn.substring(4));

		isbn = StringUtils.trimToNull(isbn);

		if (isbn == null)
			return;

		if (byISBN.containsKey(isbn)) {
			byISBN.get(isbn).add(page.getId());
		} else {
			TLongSet set = new TLongHashSet();
			set.add(page.getId());
			byISBN.put(isbn, set);
		}
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	@Override
	public void run() {
		final Map<String, TLongSet> byISBN = new HashMap<String, TLongSet>();
		final Map<String, TLongSet> byAuthor = new HashMap<String, TLongSet>();
		final Map<String, TLongSet> byBookTitle = new HashMap<String, TLongSet>();

		// for (Revision revision :
		// wikiCache.queryLatestContentByPageIds(mediaWikiBot.queryEmbeddedInPageIds(
		// "Template:Книга", Namespaces.MAIN))) {

		for (Revision revision : wikiCache.queryByEmbeddedIn("Template:Книга", new Namespace[] { Namespace.MAIN })) {

			final Page page = revision.getPage();
			final String title = page.getTitle();
			try {
				final String xmlContent = revision.getXml();
				{
					final String lowerCaseXml = xmlContent.toLowerCase();
					if (!lowerCaseXml.contains("книга") && !lowerCaseXml.contains("cite book")) {
						continue;
					}
				}

				ArticleFragment article = mediaWikiBot.getXmlParser().parse(revision);

				Map<String, List<Template>> allTemplates = article.getAllTemplates();

				if (allTemplates.containsKey("книга")) {
					for (Template template : allTemplates.get("книга")) {
						addISBN(byISBN, page, title, template.getParameterValue("isbn"));
						addAuthor(byAuthor, page, title, template.getParameterValue("автор"));
						addBookTitle(byBookTitle, page, title, template.getParameterValue("заглавие"));
					}
				}

				if (allTemplates.containsKey("cite book")) {
					for (Template template : allTemplates.get("cite book")) {
						addISBN(byISBN, page, title, template.getParameterValue("id"));
						addISBN(byISBN, page, title, template.getParameterValue("isbn"));
						addBookTitle(byBookTitle, page, title, template.getParameterValue("title"));
					}
				}
			} catch (Exception exc) {
				log.warn(title + ": " + exc.getMessage());
			}

		}

		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("__TOC__\n");

		{
			stringBuilder.append("== By ISBN ==\n");
			List<String> isbns = new ArrayList<String>(byISBN.keySet());
			Collections.sort(isbns, new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					Integer i1 = byISBN.get(o1).size();
					Integer i2 = byISBN.get(o2).size();
					return i2.compareTo(i1);
				}

			});

			for (String isbn : isbns) {
				int count = byISBN.get(isbn).size();
				if (count < 10)
					break;
				stringBuilder.append("* ISBN " + isbn + " — " + count + " articles\n");
			}
		}

		{
			stringBuilder.append("== By author ==\n");
			List<String> authors = new ArrayList<String>(byAuthor.keySet());
			Collections.sort(authors, new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					Integer i1 = byAuthor.get(o1).size();
					Integer i2 = byAuthor.get(o2).size();
					return i2.compareTo(i1);
				}

			});

			for (String author : authors) {
				int count = byAuthor.get(author).size();
				if (count < 10)
					break;
				stringBuilder.append("* " + author + " — " + count + " articles\n");
			}
		}

		{
			stringBuilder.append("== By title ==\n");
			List<String> bookTitles = new ArrayList<String>(byBookTitle.keySet());
			Collections.sort(bookTitles, new Comparator<String>() {

				@Override
				public int compare(String o1, String o2) {
					Integer i1 = byBookTitle.get(o1).size();
					Integer i2 = byBookTitle.get(o2).size();
					return i2.compareTo(i1);
				}

			});

			for (String bookTitle : bookTitles) {
				int count = byBookTitle.get(bookTitle).size();
				if (count < 10)
					break;
				stringBuilder.append("* " + bookTitle + " — " + count + " articles\n");
			}
		}

		mediaWikiBot.writeContent("User:" + mediaWikiBot.getLogin() + "/CountBooks", null, stringBuilder.toString(),
				null, "Update most used books", true, false);
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}

}
