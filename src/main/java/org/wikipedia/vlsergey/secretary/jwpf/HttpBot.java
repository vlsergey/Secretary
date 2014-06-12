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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.net.URI;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.NoHttpResponseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.AbstractHttpClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.http.HttpManager;
import org.wikipedia.vlsergey.secretary.jwpf.actions.ContentProcessable;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.CookieException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

/**
 * 
 * @author Thomas Stock
 */
public abstract class HttpBot {

	private static final String GZIP_CONTENT_ENCODING = "gzip";

	private static final Log log = LogFactory.getLog(HttpBot.class);

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

	private AbstractHttpClient httpClient;

	@Autowired
	private HttpManager httpManager;

	private URI site;

	protected synchronized void get(final HttpGet getMethod, final ContentProcessable action) throws IOException,
			CookieException, ProcessException {
		getMethod.getParams().setParameter("http.protocol.content-charset", MediaWikiBot.ENCODING);

		getMethod.setHeader("Accept-Encoding", GZIP_CONTENT_ENCODING);

		httpClient.execute(getMethod, new ResponseHandler<Object>() {
			@Override
			public Object handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {

				try {
					action.validateReturningCookies(httpClient.getCookieStore().getCookies(), getMethod);

					// logger.debug("" + getMethod.getURI());
					// logger.debug("GET: " +
					// httpResponse.getStatusLine().toString());

					InputStream inputStream = httpResponse.getEntity().getContent();
					String contentEncoding = httpResponse.getEntity().getContentEncoding() != null ? httpResponse
							.getEntity().getContentEncoding().getValue() : "";
					if ("gzip".equalsIgnoreCase(contentEncoding))
						inputStream = new GZIPInputStream(inputStream);

					String encoding = StringUtils.substringAfter(httpResponse.getEntity().getContentType().getValue(),
							"charset=");
					String out = IoUtils.readToString(inputStream, encoding);
					action.processReturningText(getMethod, out);

					int statuscode = httpResponse.getStatusLine().getStatusCode();

					if (statuscode == HttpStatus.SC_NOT_FOUND) {
						log.warn("Not Found: " + getMethod.getRequestLine().getUri());

						throw new FileNotFoundException(getMethod.getRequestLine().getUri());
					}
				} catch (CookieException exc) {
					throw new ClientProtocolException(exc);
				} catch (ProcessException exc) {
					throw new ClientProtocolException(exc);
				}
				return null;
			}
		});

	}

	public URI getSite() {
		return site;
	}

	@PostConstruct
	public synchronized void init() {
		httpClient = httpManager.newLocalhostHttpClient();
	}

	private synchronized void onPostResponse(final ContentProcessable action, final HttpPost postMethod,
			HttpResponse response) throws IOException {
		try {
			int statuscode = response.getStatusLine().getStatusCode();
			if (action.followRedirects()
					&& (statuscode == HttpStatus.SC_MOVED_TEMPORARILY || statuscode == HttpStatus.SC_MOVED_PERMANENTLY
							|| statuscode == HttpStatus.SC_SEE_OTHER || statuscode == HttpStatus.SC_TEMPORARY_REDIRECT)) {
				/*
				 * Usually a successful form-based login results in a redicrect
				 * to another url
				 */
				Header header = response.getFirstHeader("location");
				if (header != null) {
					String newuri = header.getValue();
					if ((newuri == null) || (newuri.equals(""))) {
						newuri = "/";
					}
					log.debug("Redirect target: " + newuri);

					HttpPost redirect = new HttpPost(newuri);
					redirect.setEntity(postMethod.getEntity());
					redirect.setHeader("Accept-Encoding", GZIP_CONTENT_ENCODING);
					log.trace("GET: " + redirect.getURI());
					httpClient.execute(redirect, new ResponseHandler<Object>() {
						@Override
						public Object handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
							// no more redirects?
							onPostResponse(action, postMethod, response);
							return null;
						}
					});
					return;
				}
			}

			final Header databaseLag = response.getFirstHeader("X-Database-Lag");
			final Header retryAfter = response.getFirstHeader("Retry-After");
			if (databaseLag != null) {
				throw new DatabaseLagException(databaseLag, retryAfter);
			}

			InputStream inputStream = response.getEntity().getContent();
			String out;
			try {
				String encoding = response.getFirstHeader("Content-Encoding") != null ? response.getFirstHeader(
						"Content-Encoding").getValue() : "";
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

			action.processReturningText(postMethod, out);

			action.validateReturningCookies(httpClient.getCookieStore().getCookies(), postMethod);

			log.trace(postMethod.getURI() + " || " + "POST: " + response.getStatusLine().toString());
		} catch (CookieException exc) {
			throw new ClientProtocolException(exc);
		} catch (ProcessException exc) {
			throw new ClientProtocolException(exc);
		}
	}

	protected final String performAction(final ContentProcessable contentProcessable) throws ActionException,
			ProcessException {
		List<HttpRequestBase> msgs = contentProcessable.getMessages();
		String out = "";
		Iterator<HttpRequestBase> it = msgs.iterator();
		while (it.hasNext()) {
			HttpRequestBase httpMethod = it.next();
			if (getSite() != null) {

				URI uri = httpMethod.getURI();
				if (!uri.getPath().startsWith("/wiki/")) {
					try {
						String str = getSite().getScheme() + "://" + getSite().getHost()
								+ (getSite().getPort() == -1 ? "" : ":" + getSite().getPort()) + getSite().getPath()
								+ uri.getPath() + (uri.getRawQuery() != null ? ("?" + uri.getRawQuery()) : "");
						uri = new URI(str);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					httpMethod.setURI(uri);
				} else {
					try {
						String str = getSite().getScheme() + "://" + getSite().getHost()
								+ (getSite().getPort() == -1 ? "" : ":" + getSite().getPort()) + uri.getPath()
								+ (uri.getRawQuery() != null ? ("?" + uri.getRawQuery()) : "");
						uri = new URI(str);
					} catch (Exception e) {
						throw new RuntimeException(e);
					}
					httpMethod.setURI(uri);
				}

				// logger.debug("path is: " + httpMethod.getURI());
			}

			try {
				while (true) {
					try {
						if (httpMethod instanceof HttpGet) {
							get((HttpGet) httpMethod, contentProcessable);
						} else {
							post((HttpPost) httpMethod, contentProcessable);
						}
						break;
					} catch (NoHttpResponseException exc) {
						log.info("NoHttpResponseException, wait 6 seconds");
						try {
							Thread.sleep(5 * 1000);
						} catch (InterruptedException e) {
						}
					} catch (SocketException exc) {
						log.info("SocketException, wait 6 seconds");
						try {
							Thread.sleep(5 * 1000);
						} catch (InterruptedException e) {
						}
					} catch (DatabaseLagException exc) {
						log.info("Database lag occured: " + exc.databaseLag);
						int retryAfter = 6;
						try {
							retryAfter = Integer.parseInt(exc.retryAfter.getValue());
						} catch (Exception exc2) {
							// ignore
						}
						if (retryAfter != 0) {
							log.info("Waiting for " + retryAfter + " seconds");
							try {
								Thread.sleep(retryAfter * 1000);
							} catch (InterruptedException e) {
							}
						}
					}
				}
			} catch (IOException e1) {
				throw new ActionException(e1);
			}

		}
		return out;
	}

	protected synchronized void post(final HttpPost postMethod, final ContentProcessable action) throws IOException,
			ProcessException, CookieException {
		postMethod.getParams().setParameter("http.protocol.content-charset", MediaWikiBot.ENCODING);

		postMethod.setHeader("Accept-Encoding", GZIP_CONTENT_ENCODING);

		httpClient.execute(postMethod, new ResponseHandler<Object>() {
			@Override
			public Object handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
				onPostResponse(action, postMethod, response);
				return null;
			}
		});
	}

	public void setHttpManager(HttpManager httpManager) {
		this.httpManager = httpManager;
	}

	public void setSite(URI site) {
		this.site = site;
	}
}
