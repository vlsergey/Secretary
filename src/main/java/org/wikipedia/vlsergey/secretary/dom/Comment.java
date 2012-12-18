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

import java.util.Collections;
import java.util.List;

public class Comment extends AbstractContainer {

	private static final long serialVersionUID = 1L;

	private Content content;

	public Comment(Content content) {
		this.content = content;
	}

	public Comment(String content) {
		this.content = new Text("<!--" + content + "-->");
	}

	@Override
	public List<? extends Content> getChildren() {
		return Collections.singletonList(getContent());
	}

	public Content getContent() {
		return content;
	}

	public void setContent(Content content) {
		this.content = content;
	}

	@Override
	public String toWiki(boolean removeComments) {
		return content.toWiki(removeComments);
	}

}
