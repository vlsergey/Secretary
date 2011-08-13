package org.wikipedia.vlsergey.secretary.dom.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Math;
import org.wikipedia.vlsergey.secretary.dom.NoWiki;
import org.wikipedia.vlsergey.secretary.dom.Parameter;
import org.wikipedia.vlsergey.secretary.dom.Section;
import org.wikipedia.vlsergey.secretary.dom.Table;
import org.wikipedia.vlsergey.secretary.dom.TableCell;
import org.wikipedia.vlsergey.secretary.dom.TableColumnBreak;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.Text;

public class Parser extends AbstractParser {

	@Override
	protected ArticleFragment newArticleFragment(List<Content> contents) {
		return new ArticleFragment(contents);
	}

	@Override
	protected Math newMath(String text) {
		return new Math(text);
	}

	@Override
	protected NoWiki newNoWiki(String escapedText) {
		return new NoWiki(escapedText);
	}

	@Override
	protected Parameter newParameter(Content value) {
		return new Parameter(null, value);
	}

	@Override
	protected Parameter newParameter(final Content name, final Content value) {
		return new Parameter(name, value);
	}

	@Override
	protected Section newSection(int level, Content header,
			Text afterHeaderSpaces, Content content) {

		ArticleFragment articleFragment = content instanceof ArticleFragment ? (ArticleFragment) content
				: new ArticleFragment(Arrays.asList(new Content[] { content }));

		return new Section(level, header, afterHeaderSpaces, articleFragment);
	}

	@Override
	protected Table newTable(Content tableAttributes, List<Content> tableRows,
			Text tableTail) {
		return new Table(tableAttributes, tableRows, tableTail);
	}

	@Override
	protected TableCell newTableCell(Content border, Content cellAttributes,
			Content cellAttributesBorder, Content content) {
		return new TableCell(border, cellAttributes, cellAttributesBorder,
				content);
	}

	@Override
	protected TableColumnBreak newTableColumnBreak(Content border,
			Content attributes, Text tail) {
		return new TableColumnBreak(border, attributes, tail);
	}

	@Override
	protected Template newTemplate(final Content templateName,
			List<Parameter> parameters) {

		return new Template(templateName,
				parameters != null ? new ArticleFragment(
						new ArrayList<Content>(parameters)) : null);
	}

	@Override
	protected Text newText(String text) {
		return new Text(text);
	}

}
