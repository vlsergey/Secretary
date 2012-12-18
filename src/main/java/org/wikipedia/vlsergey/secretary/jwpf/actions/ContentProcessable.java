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

import java.util.List;

import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.wikipedia.vlsergey.secretary.jwpf.utils.CookieException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

/**
 * 
 * @author Thomas Stock
 * 
 */
public interface ContentProcessable {

	public boolean followRedirects();

	/**
	 * @return the of messages in this action
	 */
	List<HttpRequestBase> getMessages();

	/**
	 * 
	 * @param s
	 *            the returning text
	 * @param hm
	 *            a
	 * @return the returning text or a modification of it
	 * @throws ProcessException
	 *             on internal problems of implementing class
	 */
	void processReturningText(HttpRequestBase hm, final String s)
			throws ProcessException;

	/**
	 * 
	 * @param cs
	 *            a
	 * @param hm
	 *            a
	 * @throws CookieException
	 *             on problems with cookies
	 */
	void validateReturningCookies(final List<Cookie> cs, HttpRequestBase hm)
			throws CookieException;

}
