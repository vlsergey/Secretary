package org.wikipedia.vlsergey.secretary.utils;

import java.io.StringWriter;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Node;

public class XmlUtils {
	public static String format(Node node) throws TransformerException {
		final StringWriter writer = new StringWriter();
		format(new DOMSource(node), false, new StreamResult(writer));
		return writer.toString();
	}

	public static void format(Source source, boolean omitDeclaration,
			Result result) throws TransformerException {
		Transformer transformer = TransformerFactory.newInstance()
				.newTransformer();
		transformer.setOutputProperty(OutputKeys.ENCODING, "utf-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");
		transformer.setOutputProperty(OutputKeys.METHOD, "xml");
		try {
			transformer.setOutputProperty(
					"{http://xml.apache.org/xslt}indent-amount", "4");
		} catch (IllegalArgumentException exc) {
			// not supported
		}

		if (omitDeclaration) {
			transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
					"yes");
		}
		transformer.transform(source, result);
	}

}
