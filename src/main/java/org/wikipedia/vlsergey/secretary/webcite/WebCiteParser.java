package org.wikipedia.vlsergey.secretary.webcite;

import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.cache.XmlCache;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Extension;
import org.wikipedia.vlsergey.secretary.dom.parser.XmlParser;

public class WebCiteParser extends XmlParser {

	private XmlCache xmlCache;

	public XmlCache getXmlCache() {
		return xmlCache;
	}

	@Override
	protected Extension parseExtension(Element extElement) {
		Extension extension = super.parseExtension(extElement);

		if (extension.getName().toWiki(true).trim().equals("ref") && extension.getInner() != null) {

			try {
				// need additional parsing of included wikitext
				final String wikiText = extension.getInner().toWiki(false);
				final String xml = xmlCache.getXml(wikiText);
				final Content innerParsed = parseContainer(xml);
				extension.setInner(innerParsed);
			} catch (Exception exc) {
				throw new RuntimeException("Unable to parse inner content of REF: " + exc, exc);
			}
		}

		return extension;
	}

	public void setXmlCache(XmlCache xmlCache) {
		this.xmlCache = xmlCache;
	}

}
