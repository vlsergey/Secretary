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
 * Philipp Kohl 
 */
package org.wikipedia.vlsergey.secretary.jwpf;

import java.net.URI;

import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.wikipedia.vlsergey.secretary.jwpf.actions.ContentProcessable;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

/**
 * 
 * @author Thomas Stock
 * 
 */
public abstract class HttpBot {

	private final HttpActionClient cc;

	private final AbstractHttpClient client;

	protected HttpBot(final URI uri) {
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", 80, PlainSocketFactory
				.getSocketFactory()));
		registry.register(new Scheme("https", 443, SSLSocketFactory
				.getSocketFactory()));

		ThreadSafeClientConnManager threadSafeClientConnManager = new ThreadSafeClientConnManager(
				registry);
		threadSafeClientConnManager.setDefaultMaxPerRoute(2);
		threadSafeClientConnManager.setMaxTotal(10);
		client = new DefaultHttpClient(threadSafeClientConnManager);

		client.getParams().setParameter("http.useragent", "Secretary/JWBF");
		cc = new HttpActionClient(client, uri);
	}

	/**
	 * 
	 * @return a
	 */
	public final AbstractHttpClient getClient() {
		return client;
	}

	// /**
	// * Simple method to get plain HTML or XML data e.g. from custom
	// specialpages
	// * or xml newsfeeds.
	// *
	// * @param u
	// * url like index.php?title=Main_Page
	// * @return HTML content
	// * @throws ActionException
	// * on any requesing problems
	// */
	// public String getPage(String u) throws ActionException {
	//
	// GetPage gp = new GetPage(u);
	//
	// try {
	// performAction(gp);
	// } catch (ProcessException e) {
	// e.printStackTrace();
	// }
	//
	// return gp.getText();
	// }

	/**
	 * 
	 * @param a
	 *            a
	 * @throws ActionException
	 *             on problems with http, cookies and io
	 * @return text
	 * @throws ProcessException
	 *             on problems in the subst of ContentProcessable
	 */
	public final String performAction(final ContentProcessable a)
			throws ActionException, ProcessException {
		return cc.performAction(a);
	}

}
