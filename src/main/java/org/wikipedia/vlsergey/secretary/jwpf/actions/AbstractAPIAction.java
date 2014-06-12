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
 * Created on 30.03.2008
 */
package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.AbstractList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ApiException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.CookieException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.xml.sax.InputSource;

public abstract class AbstractAPIAction implements ContentProcessable {

	protected static class ListAdapter<T extends Node> extends AbstractList<T> {

		private final NodeList nodeList;

		public ListAdapter(NodeList nodeList) {
			this.nodeList = nodeList;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T get(int index) {
			if (index < 0 || index >= nodeList.getLength())
				throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size());

			return (T) nodeList.item(index);
		}

		@Override
		public int size() {
			return nodeList.getLength();
		}

	}

	protected static Log log = LogFactory.getLog(AbstractAPIAction.class);

	public static final int MAXLAG = 0;

	private static final SimpleDateFormat timestampDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

	static {
		timestampDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	protected static String encode(String string) {
		try {
			String result = URLEncoder.encode(string, MediaWikiBot.CHARSET.name());
			return result;
		} catch (UnsupportedEncodingException e) {
			throw new Error("MediaWiki '" + MediaWikiBot.CHARSET.name() + "' charset not supported by Java VM");
		}

	}

	protected static synchronized String format(Date date) {
		return timestampDateFormat.format(date);
	}

	protected static String getText(Element element) {
		StringBuilder content = new StringBuilder();
		for (Node child : new ListAdapter<Node>(element.getChildNodes())) {
			if (child instanceof Text) {
				Text text = (Text) child;
				content.append(text.getTextContent());
			}
		}
		return content.toString();
	}

	protected static synchronized Date parseDate(String timestamp) throws ParseException {
		try {
			return timestampDateFormat.parse(timestamp);
		} catch (NumberFormatException exc) {
			NumberFormatException exc2 = new NumberFormatException("Unable to parse '" + timestamp + "': "
					+ exc.getMessage());
			exc2.initCause(exc);
			throw exc2;
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, Date value) {
		try {
			multipartEntity.addPart(name, new StringBody(format(value), MediaWikiBot.CHARSET));
		} catch (UnsupportedEncodingException e) {
			throw new Error("MediaWiki '" + MediaWikiBot.ENCODING + "' charset not supported by Java VM");
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, int value) {
		try {
			multipartEntity.addPart(name, new StringBody(Integer.toString(value)));
		} catch (UnsupportedEncodingException e) {
			throw new Error("MediaWiki '" + MediaWikiBot.ENCODING + "' charset not supported by Java VM");
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, String value) {
		try {
			multipartEntity.addPart(name, new StringBody(value, MediaWikiBot.CHARSET));
		} catch (UnsupportedEncodingException e) {
			throw new Error("MediaWiki '" + MediaWikiBot.CHARSET.name() + "' charset not supported by Java VM");
		}
	}

	protected final boolean bot;

	protected List<HttpRequestBase> msgs = new Vector<HttpRequestBase>();

	public AbstractAPIAction(boolean bot) {
		super();
		this.bot = bot;
	}

	protected void appendParameters(StringBuilder stringBuilder, Iterable<? extends Object> parameters) {
		try {
			String params = toStringParameters(parameters);
			params = URLEncoder.encode(params, MediaWikiBot.CHARSET.name());

			stringBuilder.append(params);
		} catch (UnsupportedEncodingException exc) {
			log.error("MediaWiki encoding not supported: '" + MediaWikiBot.CHARSET + "': " + exc.getMessage(), exc);
			throw new Error(exc.getMessage(), exc);
		}
	}

	@Override
	public boolean followRedirects() {
		return true;
	}

	@Override
	public final List<HttpRequestBase> getMessages() {
		return msgs;
	}

	protected boolean isBot() {
		return bot;
	}

	protected abstract void parseAPI(final Element root) throws ProcessException, ParseException;

	protected void parseResult(final String xml) throws ProcessException {
		try {
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = documentBuilder.parse(new InputSource(new StringReader(xml)));

			{
				List<Element> errors = new ListAdapter<Element>(document.getDocumentElement().getElementsByTagName(
						"error"));
				if (!errors.isEmpty()) {
					Element error = errors.get(0);
					throw new ApiException(error.getAttribute("code"), error.getAttribute("info"));
				}
			}

			parseAPI(document.getDocumentElement());
		} catch (Exception exc) {
			throw new ProcessException(exc);
		}
	}

	@Override
	public final void processReturningText(final HttpRequestBase hm, final String s) throws ProcessException {
		parseResult(s);
	}

	protected void setFormatXml(MultipartEntity multipartEntity) {
		setParameter(multipartEntity, "format", "xml");
	}

	protected void setMaxLag(MultipartEntity multipartEntity) {
		setParameter(multipartEntity, "maxlag", MAXLAG);
	}

	protected String toStringParameters(int[] parameters) {
		StringBuilder stringBuilder = new StringBuilder();

		boolean first = true;
		for (Object l : parameters) {
			if (!first)
				stringBuilder.append("|");
			stringBuilder.append(l.toString());

			first = false;
		}

		return stringBuilder.toString();
	}

	protected String toStringParameters(Iterable<? extends Object> parameters) {
		return toStringParameters(parameters, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	protected String toStringParameters(Iterable<? extends Object> parameters, int maxForNonBots, int maxForBots) {
		StringBuilder stringBuilder = new StringBuilder();

		int counter = 0;
		boolean first = true;
		for (Object l : parameters) {

			if (isBot() ? (counter == maxForBots) : (counter == maxForNonBots)) {
				throw new IllegalArgumentException("Too many values supplied");
			}

			if (!first)
				stringBuilder.append("|");
			stringBuilder.append(l.toString());

			first = false;
			counter++;
		}

		return stringBuilder.toString();
	}

	protected String toStringParameters(Namespace[] namespaces) {
		StringBuilder stringBuilder = new StringBuilder();

		boolean first = true;
		for (Namespace namespace : namespaces) {
			if (!first)
				stringBuilder.append("|");
			stringBuilder.append(namespace.id);

			first = false;
		}

		return stringBuilder.toString();
	}

	protected String toStringParameters(Object[] parameters) {
		StringBuilder stringBuilder = new StringBuilder();

		boolean first = true;
		for (Object l : parameters) {
			if (!first)
				stringBuilder.append("|");
			stringBuilder.append(l.toString());

			first = false;
		}

		return stringBuilder.toString();
	}

	@Override
	public void validateReturningCookies(List<Cookie> cs, HttpRequestBase hm) throws CookieException {
		// no op
	}

}