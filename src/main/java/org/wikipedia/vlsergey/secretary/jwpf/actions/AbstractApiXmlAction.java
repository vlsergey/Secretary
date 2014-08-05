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
import java.text.ParseException;
import java.util.AbstractList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ApiException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.xml.sax.InputSource;

public abstract class AbstractApiXmlAction extends AbstractApiAction {

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

	public AbstractApiXmlAction(boolean bot) {
		super(bot);
	}

	protected abstract void parseAPI(final Element root) throws ProcessException, ParseException;

	@Override
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

	protected void setFormatXml(MultipartEntity multipartEntity) {
		setParameter(multipartEntity, "format", "xml");
	}

}