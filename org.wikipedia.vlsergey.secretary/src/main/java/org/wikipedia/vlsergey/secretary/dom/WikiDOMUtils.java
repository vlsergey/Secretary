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
 * Created on 26.03.2008
 */
package org.wikipedia.vlsergey.secretary.dom;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class WikiDOMUtils {

	public static Section getFirstSectionWithArticleLink(
			ArticleFragment articleFragment, String articleTitle) {
		for (Section section : articleFragment.getSections()) {
			if (hasArticleLink(section.getHeader(), articleTitle))
				return section;

			Section child = getFirstSectionWithArticleLink(
					section.getContent(), articleTitle);
			if (child != null)
				return child;
		}
		return null;
	}

	public static boolean hasArticleLink(Content content, String articleTitle) {
		articleTitle = articleTitle.toLowerCase();
		articleTitle = articleTitle.replace(" ", "_");

		// XXX: Until Link content introduced
		if (content instanceof Text) {
			Pattern pattern = Pattern.compile("\\[\\[[ \\t]*"
					+ Pattern.quote(articleTitle) + "[ \\t]*((\\]\\])|(\\|))");

			String text = ((Text) content).getText().toLowerCase();
			text = text.replace(" ", "_");
			Matcher matcher = pattern.matcher(text);
			return matcher.find();
		}

		if (content instanceof AbstractContainer) {
			for (Content child : ((AbstractContainer) content).getChildren()) {
				if (hasArticleLink(child, articleTitle))
					return true;
			}
		}

		return false;
	}

	public static void trim(Content content) {
		trimLeft(content);
		trimRight(content);
	}

	public static void trimLeft(Content content) {
		if (content instanceof ArticleFragment) {
			final List<? extends Content> children = ((ArticleFragment) content)
					.getChildren();
			if (children.size() != 0)
				trimLeft(children.get(0));

			return;
		} else if (content instanceof Text) {
			Text text = (Text) content;
			String string = text.getText();
			string = StringUtils.stripStart(string, " \t\n\r");
			text.setText(string);

			return;
		} else if (content instanceof Section || content instanceof Template) {
			return;
		}

		throw new UnsupportedOperationException(content.getClass().getName());
	}

	public static void trimRight(Content content) {
		if (content instanceof ArticleFragment) {
			final List<? extends Content> children = ((ArticleFragment) content)
					.getChildren();
			if (children.size() != 0)
				trimRight(children.get(children.size() - 1));

			return;
		} else if (content instanceof Text) {
			Text text = (Text) content;
			String string = text.getText();
			string = StringUtils.stripEnd(string, " \t\n\r");
			text.setText(string);

			return;
		} else if (content instanceof Section) {
			ArticleFragment articleFragment = ((Section) content).getContent();
			trimRight(articleFragment);
			articleFragment.getChildren().add(new Text("\n"));
			return;
		} else if (content instanceof Template) {
			return;
		}

		throw new UnsupportedOperationException(content.getClass().getName());
	}
}
