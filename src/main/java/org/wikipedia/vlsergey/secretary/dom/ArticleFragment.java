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
 * Created on 27.02.2008
 */
package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class ArticleFragment extends AbstractContainer {

	private static final long serialVersionUID = 358081004448028714L;

	private List<Content> children;

	public ArticleFragment() {
		this.children = new ArrayList<Content>(0);
	}

	public ArticleFragment(List<? extends Content> children) {
		this.children = new ArrayList<Content>(children);
	}

	private Section findSection(final Header header) {
		List<Content> sublist = getSublist(header);
		if (sublist == null) {
			return null;
		}

		// it's back-linked to this ArticleFragment content
		return new Section(header) {

			private static final long serialVersionUID = 1L;

			private List<Content> sublist = getSublist(header);

			@Override
			public ArticleFragment getContent() {

				try {
					sublist.size();
				} catch (ConcurrentModificationException exc) {
					sublist = getSublist(header);
				}

				final ArticleFragment result = new ArticleFragment();
				result.children = sublist;
				return result;
			}
		};
	}

	public Section findSection(String sectionName) {

		for (Content content : getChildren()) {
			if (content instanceof Header) {
				if (sectionName.equals(((Header) content).getName())) {
					return findSection((Header) content);
				}
			}
		}

		return null;
	}

	public List<Section> findTopLevelSections(int level) {
		List<Section> sections = new ArrayList<Section>();

		// be aware of O(N^2)
		for (Content content : getChildren()) {
			if (content instanceof Header) {
				if (level == ((Header) content).getLevel()) {
					sections.add(findSection((Header) content));
				}
			}
		}

		return sections;
	}

	@Override
	public List<Content> getChildren() {
		return children;
	}

	private List<Content> getSublist(Header header) {
		int start = children.indexOf(header);
		if (start == -1) {
			// not found
			return null;
		}

		int nextHeader = -1;
		for (int i = start + 1; i < children.size(); i++) {
			Content content = children.get(i);
			if (content instanceof Header) {
				// check it's not same or higher level
				Header another = (Header) content;
				if (another.getLevel() >= header.getLevel()) {
					nextHeader = i;
					break;
				}
			}
		}

		List<Content> sublist;
		if (nextHeader == -1) {
			sublist = children.subList(start + 1, children.size());
		} else {
			sublist = children.subList(start + 1, nextHeader);
		}
		return sublist;
	}

	public void setChildren(List<Content> children) {
		this.children = children;
	}
}
