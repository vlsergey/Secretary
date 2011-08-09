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

package org.wikipedia.vlsergey.secretary.jwpf;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.wikipedia.vlsergey.secretary.jwpf.actions.ContentProcessable;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.CookieException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

/**
 * The main interaction class.
 * 
 * @author Thomas Stock
 * 
 */
public class HttpActionClient {

	private static final String GZIP_CONTENT_ENCODING = "gzip";

	private static final Log log = LogFactory.getLog(HttpActionClient.class);

	/**
	 * Returns the character set from the <tt>Content-Type</tt> header.
	 * 
	 * @param contentheader
	 *            The content header.
	 * @return String The character set.
	 */
	protected static String getContentCharSet(Header contentheader) {
		log.trace("enter getContentCharSet( Header contentheader )");
		String encoding = null;
		if (contentheader != null) {
			HeaderElement values[] = contentheader.getElements();
			// I expect only one header element to be there
			// No more. no less
			if (values.length == 1) {
				NameValuePair param = values[0].getParameterByName("charset");
				if (param != null) {
					// If I get anything "funny"
					// UnsupportedEncondingException will result
					encoding = param.getValue();
				}
			}
		}
		if (encoding == null) {
			encoding = MediaWikiBot.ENCODING;
			if (log.isDebugEnabled()) {
				log.debug("Default charset used: " + encoding);
			}
		}
		return encoding;
	}

	private AbstractHttpClient client;

	private URI baseURL;

	public HttpActionClient(final AbstractHttpClient client, final URI baseURL) {
		this.client = client;
		this.baseURL = baseURL;
	}

	protected void get(HttpGet getMethod, ContentProcessable cp)
			throws IOException, CookieException, ProcessException {
		showCookies(client);
		String out = "";
		getMethod.getParams().setParameter("http.protocol.content-charset",
				MediaWikiBot.ENCODING);

		getMethod.setHeader("Accept-Encoding", GZIP_CONTENT_ENCODING);

		// System.err.println(authgets.getParams().getParameter("http.protocol.content-charset"));

		HttpResponse httpResponse = client.execute(getMethod);

		cp.validateReturningCookies(client.getCookieStore().getCookies(),
				getMethod);
		log.debug(getMethod.getURI());
		log.debug("GET: " + httpResponse.getStatusLine().toString());

		InputStream inputStream = httpResponse.getEntity().getContent();
		String contentEncoding = httpResponse.getEntity().getContentEncoding() != null ? httpResponse
				.getEntity().getContentEncoding().getValue()
				: "";
		if ("gzip".equalsIgnoreCase(contentEncoding))
			inputStream = new GZIPInputStream(inputStream);

		String encoding = StringUtils.substringAfter(httpResponse.getEntity()
				.getContentType().getValue(), "charset=");
		out = IoUtils.readToString(inputStream, encoding);
		cp.processReturningText(getMethod, out);

		int statuscode = httpResponse.getStatusLine().getStatusCode();

		if (statuscode == HttpStatus.SC_NOT_FOUND) {
			log.warn("Not Found: " + getMethod.getRequestLine().getUri());

			throw new FileNotFoundException(getMethod.getRequestLine().getUri());
		}
	}

	/**
	 * 
	 * @param contentProcessable
	 * @return message, never null
	 * @throws ActionException
	 *             on problems with http, cookies and io
	 * @throws ProcessException
	 *             on inner problems
	 */
	public String performAction(ContentProcessable contentProcessable)
			throws ActionException, ProcessException {

		List<HttpRequestBase> msgs = contentProcessable.getMessages();
		String out = "";
		Iterator<HttpRequestBase> it = msgs.iterator();
		while (it.hasNext()) {
			HttpRequestBase httpMethod = it.next();
			if (baseURL != null) {

				URI uri = httpMethod.getURI();
				if (!uri.getPath().startsWith("/wiki/")) {
					try {
						String str = baseURL.getScheme()
								+ "://"
								+ baseURL.getHost()
								+ (baseURL.getPort() == -1 ? "" : ":"
										+ baseURL.getPort())
								+ baseURL.getPath()
								+ uri.getPath()
								+ (uri.getRawQuery() != null ? ("?" + uri
										.getRawQuery()) : "");
						uri = new URI(str);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					httpMethod.setURI(uri);
				} else {
					try {
						String str = baseURL.getScheme()
								+ "://"
								+ baseURL.getHost()
								+ (baseURL.getPort() == -1 ? "" : ":"
										+ baseURL.getPort())
								+ uri.getPath()
								+ (uri.getRawQuery() != null ? ("?" + uri
										.getRawQuery()) : "");
						uri = new URI(str);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					httpMethod.setURI(uri);
				}

				log.debug("path is: " + httpMethod.getURI());

			}
			try {
				if (httpMethod instanceof HttpGet) {

					get((HttpGet) httpMethod, contentProcessable);

				} else {
					post((HttpPost) httpMethod, contentProcessable);
				}
			} catch (IOException e1) {
				throw new ActionException(e1);
			}

		}
		return out;

	}

	/**
	 * Process a POST Message.
	 * 
	 * @param authpost
	 *            a
	 * @param cp
	 *            a
	 * @return a returning message, not null
	 * @throws IOException
	 *             on problems
	 * @throws ProcessException
	 *             on problems
	 * @throws CookieException
	 *             on problems
	 */
	protected void post(HttpPost httpMethod, ContentProcessable cp)
			throws IOException, ProcessException, CookieException {
		showCookies(client);
		httpMethod.getParams().setParameter("http.protocol.content-charset",
				MediaWikiBot.ENCODING);

		String out = "";

		httpMethod.setHeader("Accept-Encoding", GZIP_CONTENT_ENCODING);
		HttpResponse response = client.execute(httpMethod);

		// Usually a successful form-based login results in a redicrect to
		// another url

		int statuscode = response.getStatusLine().getStatusCode();
		if (cp.followRedirects()
				&& (statuscode == HttpStatus.SC_MOVED_TEMPORARILY
						|| statuscode == HttpStatus.SC_MOVED_PERMANENTLY
						|| statuscode == HttpStatus.SC_SEE_OTHER || statuscode == HttpStatus.SC_TEMPORARY_REDIRECT)) {
			Header header = response.getFirstHeader("location");
			if (header != null) {
				String newuri = header.getValue();
				if ((newuri == null) || (newuri.equals(""))) {
					newuri = "/";
				}
				log.debug("Redirect target: " + newuri);

				HttpPost redirect = new HttpPost(newuri);
				redirect.setEntity(httpMethod.getEntity());
				redirect.setHeader("Accept-Encoding", GZIP_CONTENT_ENCODING);

				log.info("GET: " + redirect.getURI());
				response = client.execute(redirect);

				log.debug("Redirect: " + response.getStatusLine().toString());
			}
		}

		InputStream inputStream = response.getEntity().getContent();
		try {
			String encoding = response.getFirstHeader("Content-Encoding") != null ? response
					.getFirstHeader("Content-Encoding").getValue() : "";
			if (GZIP_CONTENT_ENCODING.equalsIgnoreCase(encoding)) {
				inputStream = new GZIPInputStream(inputStream);
			}

			Header charsetHeader = response.getFirstHeader("Content-Type");
			String charset;
			if (charsetHeader == null)
				charset = MediaWikiBot.ENCODING;
			else
				charset = getContentCharSet(charsetHeader);

			out = IoUtils.readToString(inputStream, charset);
		} finally {
			inputStream.close();
		}

		cp.processReturningText(httpMethod, out);

		cp.validateReturningCookies(client.getCookieStore().getCookies(),
				httpMethod);

		log.debug(httpMethod.getURI() + " || " + "POST: "
				+ response.getStatusLine().toString());
	}

	/**
	 * send the cookies to the logger.
	 * 
	 * @param client
	 *            a
	 */
	protected void showCookies(AbstractHttpClient client) {
		List<Cookie> cookies = client.getCookieStore().getCookies();
		for (Cookie cookie : cookies) {
			log.trace("cookie: " + cookie);
		}
	}

}
