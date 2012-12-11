/*
 * Copyright 2007 Thomas Stock.
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
 * 
 */
package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;
import java.util.Vector;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.utils.CookieException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

/**
 * @author Thomas Stock
 * 
 */
public abstract class MWAction implements ContentProcessable {
	protected static String encode(String string) {
		try {
			String result = URLEncoder.encode(string, MediaWikiBot.CHARSET.name());
			return result;
		} catch (UnsupportedEncodingException e) {
			throw new Error("MediaWiki '" + MediaWikiBot.CHARSET.name() + "' charset not supported by Java VM");
		}

	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, String value) {
		try {
			multipartEntity.addPart(name, new StringBody(value, MediaWikiBot.CHARSET));
		} catch (UnsupportedEncodingException e) {
			throw new Error("MediaWiki '" + MediaWikiBot.CHARSET.name() + "' charset not supported by Java VM");
		}
	}

	protected List<HttpRequestBase> msgs;

	public MWAction() {
		msgs = new Vector<HttpRequestBase>();
	}

	@Override
	public boolean followRedirects() {
		return true;
	}

	@Override
	public final List<HttpRequestBase> getMessages() {
		return msgs;
	}

	@Override
	public void processReturningText(final HttpRequestBase hm, final String s) throws ProcessException {
		// no op
	}

	@Override
	public void validateReturningCookies(List<Cookie> cs, HttpRequestBase hm) throws CookieException {
		// no op
	}

}
