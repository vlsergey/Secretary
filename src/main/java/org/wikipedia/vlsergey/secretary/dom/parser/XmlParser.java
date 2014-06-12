package org.wikipedia.vlsergey.secretary.dom.parser;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
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
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.DocumentBuilderPool;
import org.xml.sax.InputSource;

public class XmlParser extends AbstractParser {

	@Autowired
	private DocumentBuilderPool documentBuilderPool;

	public XmlParser() {
	}

	public ArticleFragment parse(Revision revision) throws Exception {
		Document xmlDoc = documentBuilderPool.parse(new InputSource(new StringReader(revision.getXml())));
		final ParseContext context = new ParseContext();
		context.pageName = revision.getPage() != null ? revision.getPage().getTitle() : null;
		return parseRoot(context, xmlDoc.getDocumentElement());
	}

	public ArticleFragment parse(String xml) throws Exception {
		Document xmlDoc = documentBuilderPool.parse(new InputSource(new StringReader(xml)));
		return parseRoot(new ParseContext(), xmlDoc.getDocumentElement());
	}

	protected Comment parseComment(ParseContext context, Element commentElement) {
		Content content = parseContainer(context, commentElement);
		return newComment(content);
	}

	protected Content parseContainer(ParseContext context, Element containerElement) {

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
				content = parseElement(context, (Element) node);
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

	public Content parseContainer(ParseContext context, String xml) throws Exception {
		Document xmlDoc = documentBuilderPool.parse(new InputSource(new StringReader(xml)));
		return parseContainer(context, xmlDoc.getDocumentElement());
	}

	protected Content parseElement(ParseContext context, Element element) {

		if ("comment".equals(element.getNodeName())) {
			return parseComment(context, element);
		}

		if ("ext".equals(element.getNodeName())) {
			return parseExtension(context, element);
		}

		if ("h".equals(element.getNodeName())) {
			return parseHeader(context, element);
		}

		if ("ignore".equals(element.getNodeName())) {
			return parseIgnore(context, element);
		}

		if ("template".equals(element.getNodeName())) {
			return parseTemplate(context, element, false);
		}

		if ("tplarg".equals(element.getNodeName())) {
			return parseTemplate(context, element, true);
		}

		throw new UnsupportedOperationException("Unknown element: " + element.getNodeName());
	}

	protected Extension parseExtension(ParseContext context, Element extElement) {
		Content name = null;
		Content attr = null;
		Content inner = null;
		Content close = null;

		NodeList children = extElement.getChildNodes();
		for (int n = 0; n < children.getLength(); n++) {
			Node node = children.item(n);

			if (node.getNodeType() == Node.ELEMENT_NODE && "attr".equals(node.getNodeName())) {
				attr = parseContainer(context, (Element) node);
				continue;
			}

			if (node.getNodeType() == Node.ELEMENT_NODE && "name".equals(node.getNodeName())) {
				name = parseContainer(context, (Element) node);
				continue;
			}

			if (node.getNodeType() == Node.ELEMENT_NODE && "inner".equals(node.getNodeName())) {
				inner = parseContainer(context, (Element) node);
				continue;
			}

			if (node.getNodeType() == Node.ELEMENT_NODE && "close".equals(node.getNodeName())) {
				close = parseContainer(context, (Element) node);
				continue;
			}

			throw new UnsupportedOperationException("Unknown node: " + node.getNodeType() + " / " + node.getNodeName());
		}

		return newExtension(name, attr, inner, close);
	}

	protected Content parseHeader(ParseContext context, Element hElement) {
		int id = Integer.parseInt(hElement.getAttribute("i"));
		int level = Integer.parseInt(hElement.getAttribute("level"));
		Content content = parseContainer(context, hElement);

		return newHeader(level, id, content);
	}

	protected Ignore parseIgnore(ParseContext context, Element ignoreElement) {
		Content content = parseContainer(context, ignoreElement);
		return newIgnore(content);
	}

	protected ArticleFragment parseRoot(ParseContext context, Element documentElement) {
		if (!"root".equals(documentElement.getNodeName()))
			throw new ParsingException("Root element name is not 'root': " + documentElement.getNodeName());

		Content parsed = parseContainer(context, documentElement);
		if (parsed instanceof ArticleFragment) {
			return (ArticleFragment) parsed;
		}

		return new ArticleFragment(Collections.singletonList(parsed));
	}

	protected Content parseTemplate(ParseContext context, Element element, boolean templateArgument) {
		NodeList children = element.getChildNodes();

		Content title = null;
		List<TemplatePart> parts = new ArrayList<TemplatePart>(children.getLength() - 1);

		for (int n = 0; n < children.getLength(); n++) {
			Node node = children.item(n);

			if (node.getNodeType() == Node.ELEMENT_NODE && "title".equals(node.getNodeName())) {
				title = parseContainer(context, (Element) node);
				continue;
			}

			if (node.getNodeType() == Node.ELEMENT_NODE && "part".equals(node.getNodeName())) {
				TemplatePart part = parseTemplatePart(context, (Element) node);
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

	protected TemplatePart parseTemplatePart(ParseContext context, Element partElement) {
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
				name = parseContainer(context, (Element) node);
				continue;
			}

			if (node.getNodeType() == Node.ELEMENT_NODE && "value".equals(node.getNodeName())) {
				value = parseContainer(context, (Element) node);
				continue;
			}

			throw new UnsupportedOperationException("Unknown node: " + node.getNodeType() + " / " + node.getNodeName());
		}

		return newTemplatePart(name, equals, value);
	}
}
