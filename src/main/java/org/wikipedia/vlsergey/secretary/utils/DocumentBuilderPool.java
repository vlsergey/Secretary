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

import javax.xml.parsers.DocumentBuilderFactory;

import org.springframework.stereotype.Component;

/**
 * Pool of {@link javax.xml.parsers.DocumentBuilder}s neither with validation
 * nor namespace support
 * 
 * @author vlsergey {at} gmail {dot} com
 */
@Component
public class DocumentBuilderPool extends AbstractDocumentBuilderPool {

	private DocumentBuilderFactory documentBuilderFactory;

	@Override
	protected synchronized DocumentBuilderFactory getDocumentBuilderFactory() {
		if (documentBuilderFactory == null) {
			documentBuilderFactory = DocumentBuilderFactory.newInstance();
			documentBuilderFactory.setNamespaceAware(false);
			documentBuilderFactory.setValidating(false);
		}
		return documentBuilderFactory;
	}

}
