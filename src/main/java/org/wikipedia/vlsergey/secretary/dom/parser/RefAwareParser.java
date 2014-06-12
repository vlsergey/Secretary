package org.wikipedia.vlsergey.secretary.dom.parser;

import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.cache.XmlCache;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Extension;

public class RefAwareParser extends XmlParser {

	private XmlCache xmlCache;

	public XmlCache getXmlCache() {
		return xmlCache;
	}

	@Override
	protected Extension parseExtension(ParseContext context, Element extElement) {
		Extension extension = super.parseExtension(context, extElement);

		if (extension.getName().toWiki(true).trim().equals("ref") && extension.getInner() != null) {

			try {
				// need additional parsing of included wikitext
				final String wikiText = extension.getInner().toWiki(false);
				final String xml = xmlCache.getXml(context.pageName, wikiText);
				final Content innerParsed = parseContainer(context, xml);
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
