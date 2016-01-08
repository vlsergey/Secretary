/*
 * Copyright 2001-2010 Fizteh-Center Lab., MIPT, Russia
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
 * Created on 20.04.2010
 */
package org.wikipedia.vlsergey.secretary.utils;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.apache.commons.pool.impl.GenericObjectPool.Config;
import org.springframework.beans.factory.annotation.Autowired;
import org.w3c.dom.Document;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Abstract {@link DocumentBuilder} pool
 * 
 * @author vlsergey {at} gmail {dot} com
 */
public abstract class AbstractDocumentBuilderPool extends AbstractDelegatedGenericPoolComponent implements
		PoolableObjectFactory<DocumentBuilder> {

	private static final Log log = LogFactory.getLog(AbstractDocumentBuilderPool.class);

	@Autowired
	private DefaultErrorHandler defaultErrorHandler;

	// null until initialization
	private final GenericObjectPool<DocumentBuilder> pool;

	public AbstractDocumentBuilderPool() {
		Config config = new Config();
		config.maxActive = -1;
		config.maxIdle = 50;
		pool = new GenericObjectPool<DocumentBuilder>(this, config);
	}

	@Override
	public void activateObject(DocumentBuilder obj) throws Exception {

	}

	public DocumentBuilder borrow() throws ParserConfigurationException {
		try {
			return pool.borrowObject();
		} catch (ParserConfigurationException exc) {
			throw exc;
		} catch (RuntimeException exc) {
			throw exc;
		} catch (Exception exc) {
			log.error(exc.getMessage(), exc);
			throw new Error(exc);
		}
	}

	public DocumentBuilder borrow(ErrorHandler errorHandler) throws ParserConfigurationException {
		DocumentBuilder builder = borrow();
		builder.setErrorHandler(errorHandler);
		return builder;
	}

	@Override
	public void destroyObject(DocumentBuilder obj) {
	}

	protected abstract DocumentBuilderFactory getDocumentBuilderFactory();

	@Override
	protected GenericObjectPool<DocumentBuilder> getObjectPool() {
		return pool;
	}

	@Override
	public DocumentBuilder makeObject() throws ParserConfigurationException {
		final DocumentBuilder newDocumentBuilder = getDocumentBuilderFactory().newDocumentBuilder();
		newDocumentBuilder.setErrorHandler(defaultErrorHandler);
		return newDocumentBuilder;
	}

	public Document newDocument() throws ParserConfigurationException {
		DocumentBuilder builder = borrow();
		try {
			return builder.newDocument();
		} finally {
			returnObject(builder);
		}
	}

	public Document parse(File file) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder builder = borrow();
		try {
			return builder.parse(file);
		} finally {
			returnObject(builder);
		}
	}

	public Document parse(File file, ErrorHandler errorHandler) throws ParserConfigurationException, SAXException,
			IOException {
		DocumentBuilder builder = borrow();
		builder.setErrorHandler(errorHandler);
		try {
			return builder.parse(file);
		} finally {
			returnObject(builder);
		}
	}

	public Document parse(InputSource inputSource) throws ParserConfigurationException, SAXException, IOException {
		DocumentBuilder builder = borrow();
		try {
			return builder.parse(inputSource);
		} finally {
			returnObject(builder);
		}
	}

	@Override
	public void passivateObject(DocumentBuilder obj) {
		try {
			(obj).setErrorHandler(defaultErrorHandler);
			(obj).reset();
		} catch (UnsupportedOperationException exc) {
			log.warn(exc.getMessage(), exc);
		}
	}

	public Document read(File file) throws ParserConfigurationException, SAXException, IOException {
		if (file == null)
			throw new IllegalArgumentException("file");

		DocumentBuilder builder = borrow();
		try {
			return builder.parse(file);
		} finally {
			returnObject(builder);
		}
	}

	public void returnObject(DocumentBuilder documentBuilder) {
		try {
			pool.returnObject(documentBuilder);
		} catch (RuntimeException exc) {
			throw exc;
		} catch (Exception exc) {
			log.error(exc.getMessage(), exc);
			throw new Error(exc);
		}
	}

	@Override
	public boolean validateObject(DocumentBuilder obj) {
		return obj != null;
	}

}
