/*
 * Copyright 2001-2012 Fizteh-Center Lab., MIPT, Russia
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
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class Review extends AbstractAPIAction {

	public Review(boolean bot, Revision revision, String token, String comment, Boolean unapprove, Integer flag_accuracy) {
		super(bot);

		HttpPost postMethod = new HttpPost("/api.php");

		MultipartEntity multipartEntity = new MultipartEntity();
		setParameter(multipartEntity, "action", "review");
		setParameter(multipartEntity, "format", "xml");
		setParameter(multipartEntity, "token", token);

		setParameter(multipartEntity, "revid", revision.getId().toString());

		if (comment != null)
			setParameter(multipartEntity, "comment", comment);
		if (unapprove != null)
			setParameter(multipartEntity, "unapprove", "" + unapprove);
		if (flag_accuracy != null)
			setParameter(multipartEntity, "flag_accuracy", "" + flag_accuracy);

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
