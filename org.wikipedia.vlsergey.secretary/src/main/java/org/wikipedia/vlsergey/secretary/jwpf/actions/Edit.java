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
 * Created on 08.09.2008
 */
package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.text.ParseException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class Edit extends AbstractAPIAction {

	public Edit(Page page, Revision revision, String token, String text, String summary, boolean minor, boolean bot) {
		if (revision.getTimestamp() == null) {
			throw new IllegalArgumentException("Current revision must have timestamp to prevent edit conflicts");
		}

		HttpPost postMethod = new HttpPost("/api.php");

		MultipartEntity multipartEntity = new MultipartEntity();
		setParameter(multipartEntity, "action", "edit");
		setParameter(multipartEntity, "title", page.getTitle());
		setParameter(multipartEntity, "token", token);
		setParameter(multipartEntity, "text", text);

		setParameter(multipartEntity, "summary", summary);
		if (minor) {
			setParameter(multipartEntity, "minor", "1");
		} else {
			setParameter(multipartEntity, "minor", "0");
		}

		if (bot) {
			setParameter(multipartEntity, "bot", "1");
		} else {
			setParameter(multipartEntity, "bot", "0");
		}

		setParameter(multipartEntity, "basetimestamp", revision.getTimestamp());
		setParameter(multipartEntity, "nocreate", "1");

		setParameter(multipartEntity, "format", "xml");

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);
	}

	public Edit(String pageTitle, String token, String prependText, String text, String appendText, String summary,
			boolean minor, boolean bot, boolean nocreate) {

		HttpPost postMethod = new HttpPost("/api.php");

		MultipartEntity multipartEntity = new MultipartEntity();
		setParameter(multipartEntity, "action", "edit");
		setParameter(multipartEntity, "title", pageTitle);
		setParameter(multipartEntity, "token", token);

		if (prependText != null)
			setParameter(multipartEntity, "prependtext", prependText);

		if (text != null)
			setParameter(multipartEntity, "text", text);

		if (appendText != null)
			setParameter(multipartEntity, "appendtext", appendText);

		setParameter(multipartEntity, "summary", summary);
		if (minor) {
			setParameter(multipartEntity, "minor", "1");
		} else {
			setParameter(multipartEntity, "minor", "0");
		}

		if (bot) {
			setParameter(multipartEntity, "bot", "1");
		} else {
			setParameter(multipartEntity, "bot", "0");
		}

		if (nocreate) {
			setParameter(multipartEntity, "nocreate", "1");
		}

		setParameter(multipartEntity, "format", "xml");

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);
	}

	@Override
	protected void parseAPI(Element root) throws ProcessException, ParseException {
		String result = ((Element) root.getFirstChild()).getAttribute("result");

		if (!result.equals("Success"))
			throw new ProcessException(result);
	}

}
