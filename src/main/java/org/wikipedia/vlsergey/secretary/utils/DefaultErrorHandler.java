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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Default implementation of {@link ErrorHandler}
 * 
 * @author vlsergey {at} gmail {dot} com
 */
@Component
public class DefaultErrorHandler implements ErrorHandler {

	private static final Log log = LogFactory.getLog(DefaultErrorHandler.class);

	@Override
	public void error(SAXParseException exc) throws SAXException {
		if (log.isErrorEnabled()) {
			StringBuffer errorMessage = getText(exc, "error");
			log.warn(errorMessage, exc);
		}
	}

	@Override
	public void fatalError(SAXParseException exc) throws SAXException {
		if (log.isErrorEnabled()) {
			StringBuffer errorMessage = getText(exc, "fatal error");
			log.warn(errorMessage, exc);
		}
	}

	protected StringBuffer getText(SAXParseException exc, String type) {
		StringBuffer errorMessage = new StringBuffer();
		if (exc.getSystemId() != null) {
			errorMessage.append("[");
			errorMessage.append(exc.getSystemId());
			errorMessage.append("] ");
		}
		errorMessage.append("XML ");
		errorMessage.append(type);
		errorMessage.append(": ");
		errorMessage.append(exc.getMessage());

		// String around = XMLTools.getSourceTextAround(exc);
		// if (around != null) {
		// errorMessage.append(":\n");
		// errorMessage.append(around);
		// errorMessage.append("\n");
		// }
		return errorMessage;
	}

	@Override
	public void warning(SAXParseException exc) throws SAXException {
		if (log.isWarnEnabled()) {
			StringBuffer errorMessage = getText(exc, "warning");
			log.warn(errorMessage, exc);
		}
	}
}
