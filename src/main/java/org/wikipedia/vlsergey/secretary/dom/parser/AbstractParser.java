package org.wikipedia.vlsergey.secretary.dom.parser;

import java.util.List;

import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Comment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Extension;
import org.wikipedia.vlsergey.secretary.dom.Header;
import org.wikipedia.vlsergey.secretary.dom.Ignore;
import org.wikipedia.vlsergey.secretary.dom.Math;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplateArgument;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.dom.Text;

public abstract class AbstractParser {

	protected ArticleFragment newArticleFragment(List<Content> contents) {
		return new ArticleFragment(contents);
	}

	protected Comment newComment(Content content) {
		return new Comment(content);
	}

	protected Extension newExtension(Content name, Content attr, Content inner, Content close) {
		return new Extension(name, attr, inner, close);
	}

	protected Header newHeader(int level, int id, Content content) {
		return new Header(level, id, content);
	}

	protected Ignore newIgnore(Content content) {
		return new Ignore(content);
	}

	protected Math newMath(String text) {
		return new Math(text);
	}

	protected Extension newNoWiki(String escapedText) {
		return new Extension(newText("nowiki"), null, newText(escapedText), new Text("</nowiki>"));
	}

	protected Template newTemplate(final Content title, List<TemplatePart> parameters) {
		return new Template(title, parameters);
	}

	protected TemplateArgument newTemplateArgument(final Content title, List<TemplatePart> parameters) {
		return new TemplateArgument(title, parameters);
	}

	protected TemplatePart newTemplatePart(Content value) {
		return new TemplatePart(null, value);
	}

	protected TemplatePart newTemplatePart(final Content name, final Content value) {
		return new TemplatePart(name, value);
	}

	protected TemplatePart newTemplatePart(final Content name, final String equals, final Content value) {
		return new TemplatePart(name, equals, value);
	}

	protected Text newText(String text) {
		return new Text(text);
	}

}
