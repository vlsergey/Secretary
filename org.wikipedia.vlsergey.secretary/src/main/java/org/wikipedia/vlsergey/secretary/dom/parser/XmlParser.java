package org.wikipedia.vlsergey.secretary.dom.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Comment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Extension;
import org.wikipedia.vlsergey.secretary.dom.Ignore;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.dom.Text;
import org.xml.sax.InputSource;

public class XmlParser extends AbstractParser {

	private final DocumentBuilderFactory documentBuilderFactory;

	public XmlParser() {
		documentBuilderFactory = DocumentBuilderFactory.newInstance();
	}

	public ArticleFragment parse(String xml) throws Exception {
		Document xmlDoc = documentBuilderFactory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
		return parseRoot(xmlDoc.getDocumentElement());
	}

	private Comment parseComment(Element commentElement) {
		Content content = parseContainer(commentElement);
		return newComment(content);
	}

	private Content parseContainer(Element containerElement) {

		if (containerElement.getChildNodes().getLength() == 0) {
			return null;
		}

		NodeList children = containerElement.getChildNodes();
		List<Content> list = new ArrayList<Content>(children.getLength());
		for (int n = 0; n < children.getLength(); n++) {
			Node node = children.item(n);

			Content content;
			switch (node.getNodeType()) {

			case Node.ELEMENT_NODE:
				content = parseElement((Element) node);
				break;

			case Node.TEXT_NODE:
				content = new Text(node.getNodeValue());
				break;

			default:
				throw new UnsupportedOperationException("Unknown node type: " + node.getNodeType());

			}

			if (content != null) {
				list.add(content);
			}
		}

		if (list.size() == 1) {
			return list.get(0);
		}

		return newArticleFragment(list);
	}

	private Content parseElement(Element element) {

		if ("comment".equals(element.getNodeName())) {
			return parseComment(element);
		}

		if ("ext".equals(element.getNodeName())) {
			return parseExtension(element);
		}

		if ("h".equals(element.getNodeName())) {
			return parseHeader(element);
		}

		if ("ignore".equals(element.getNodeName())) {
			return parseIgnore(element);
		}

		if ("template".equals(element.getNodeName())) {
			return parseTemplate(element, false);
		}

		if ("tplarg".equals(element.getNodeName())) {
			return parseTemplate(element, true);
		}

		throw new UnsupportedOperationException("Unknown element: " + element.getNodeName());
	}

	private Extension parseExtension(Element extElement) {
		Content name = null;
		Content attr = null;
		Content inner = null;
		Content close = null;

		NodeList children = extElement.getChildNodes();
		for (int n = 0; n < children.getLength(); n++) {
			Node node = children.item(n);

			if (node.getNodeType() == Node.ELEMENT_NODE && "attr".equals(node.getNodeName())) {
				attr = parseContainer((Element) node);
				continue;
			}

			if (node.getNodeType() == Node.ELEMENT_NODE && "name".equals(node.getNodeName())) {
				name = parseContainer((Element) node);
				continue;
			}

			if (node.getNodeType() == Node.ELEMENT_NODE && "inner".equals(node.getNodeName())) {
				inner = parseContainer((Element) node);
				continue;
			}

			if (node.getNodeType() == Node.ELEMENT_NODE && "close".equals(node.getNodeName())) {
				close = parseContainer((Element) node);
				continue;
			}

			throw new UnsupportedOperationException("Unknown node: " + node.getNodeType() + " / " + node.getNodeName());
		}

		return newExtension(name, attr, inner, close);
	}

	private Content parseHeader(Element hElement) {
		int id = Integer.parseInt(hElement.getAttribute("i"));
		int level = Integer.parseInt(hElement.getAttribute("level"));
		Content content = parseContainer(hElement);

		return newHeader(level, id, content);
	}

	private Ignore parseIgnore(Element ignoreElement) {
		Content content = parseContainer(ignoreElement);
		return newIgnore(content);
	}

	private ArticleFragment parseRoot(Element documentElement) {
		if (!"root".equals(documentElement.getNodeName()))
			throw new ParsingException("Root element name is not 'root': " + documentElement.getNodeName());

		Content parsed = parseContainer(documentElement);
		if (parsed instanceof ArticleFragment) {
			return (ArticleFragment) parsed;
		}

		return new ArticleFragment(Collections.singletonList(parsed));
	}

	private Content parseTemplate(Element element, boolean templateArgument) {
		NodeList children = element.getChildNodes();

		Content title = null;
		List<TemplatePart> parts = new ArrayList<TemplatePart>(children.getLength() - 1);

		for (int n = 0; n < children.getLength(); n++) {
			Node node = children.item(n);

			if (node.getNodeType() == Node.ELEMENT_NODE && "title".equals(node.getNodeName())) {
				title = parseContainer((Element) node);
				continue;
			}

			if (node.getNodeType() == Node.ELEMENT_NODE && "part".equals(node.getNodeName())) {
				TemplatePart part = parseTemplatePart((Element) node);
				if (part != null) {
					parts.add(part);
				}
				continue;
			}

			throw new UnsupportedOperationException("Unknown node: " + node.getNodeType() + " / " + node.getNodeName());
		}

		if (templateArgument) {
			return newTemplateArgument(title, parts);
		}

		return newTemplate(title, parts);
	}

	private TemplatePart parseTemplatePart(Element partElement) {
		Content name = null;
		String equals = null;
		Content value = null;

		NodeList children = partElement.getChildNodes();
		for (int n = 0; n < children.getLength(); n++) {
			Node node = children.item(n);

			if (node.getNodeType() == Node.TEXT_NODE && "=".equals(node.getNodeValue())) {
				equals = node.getNodeValue();
				continue;
			}

			if (node.getNodeType() == Node.ELEMENT_NODE && "name".equals(node.getNodeName())) {
				name = parseContainer((Element) node);
				continue;
			}

			if (node.getNodeType() == Node.ELEMENT_NODE && "value".equals(node.getNodeName())) {
				value = parseContainer((Element) node);
				continue;
			}

			throw new UnsupportedOperationException("Unknown node: " + node.getNodeType() + " / " + node.getNodeName());
		}

		return newTemplatePart(name, equals, value);
	}
}
