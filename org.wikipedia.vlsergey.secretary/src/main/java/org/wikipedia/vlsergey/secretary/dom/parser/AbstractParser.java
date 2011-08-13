package org.wikipedia.vlsergey.secretary.dom.parser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.lang.StringUtils;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Comment;
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

public abstract class AbstractParser {

	protected interface BorderListener {

		void onInside(int startIncluded, int endExcluded);

		void onOutside(int startIncluded, int endExcluded);
	}

	protected class ProcessObject {

		final HashMap<Integer, Content> byPosition;

		final String workString;

		public ProcessObject(String workString,
				HashMap<Integer, Content> byPosition) {
			this.workString = workString;
			this.byPosition = byPosition;

			for (Integer position : byPosition.keySet()) {
				if (workString.charAt(position) != '.')
					throw new AssertionError();
			}
		}

		protected void copySubObjectInto(int startIncluded, int endExcluded,
				final StringBuilder stringBuilder,
				final LinkedHashMap<Integer, Content> byPosition) {
			int difference = startIncluded - stringBuilder.length();

			for (Entry<Integer, Content> entry : this.byPosition.entrySet()) {
				final Integer position = entry.getKey();

				if (position < startIncluded || position >= endExcluded)
					continue;

				byPosition.put(position - difference, entry.getValue());
			}

			stringBuilder.append(this.workString, startIncluded, endExcluded);
		}

		protected ProcessObject subObject(int startIncluded, int endExcluded) {
			final String string = this.workString.substring(startIncluded,
					endExcluded);
			final LinkedHashMap<Integer, Content> byPosition = new LinkedHashMap<Integer, Content>();

			for (final Entry<Integer, Content> entry : this.byPosition
					.entrySet()) {
				final Integer position = entry.getKey();
				final Content content = entry.getValue();

				if (position >= startIncluded && position < endExcluded) {
					byPosition.put(position - startIncluded, content);
				}
			}
			return new ProcessObject(string, byPosition);
		}

		protected Content toContent() {
			List<Content> result = new ArrayList<Content>(
					byPosition.size() * 2 + 1);

			if (byPosition.isEmpty()) {
				return newText(workString);
			}

			int prevPosition = 0;
			for (Entry<Integer, Content> entry : byPosition.entrySet()) {
				final Integer position = entry.getKey();

				if (prevPosition != position.intValue()) {
					String substring = workString.substring(prevPosition,
							position);
					result.add(newText(substring));
				}

				final Content content = entry.getValue();
				result.add(content);

				// we skip content marker
				prevPosition = position.intValue() + 1;
			}

			if (prevPosition < workString.length()) {
				result.add(newText(workString.substring(prevPosition)));
			}

			return newArticleFragment(result);
		}

		@Override
		public String toString() {
			return workString;
		}
	}

	private final static Pattern patternHeader;

	static {
		try {
			patternHeader = Pattern.compile("^(\\=+.*=+)([ \\t]*)($)",
					Pattern.MULTILINE);
		} catch (PatternSyntaxException exc) {
			throw new ExceptionInInitializerError(exc);
		}
	}

	protected static void extractByBorder(String text, String leftBorder,
			String rightBorder, BorderListener listener) {
		int position = 0;
		while (text.indexOf(leftBorder, position) != -1) {

			if (text.indexOf(rightBorder, position) == -1)
				throw new ParsingException("Unmatched " + leftBorder + " in "
						+ StringUtils.substring(text, position, position + 100));

			if (text.indexOf(rightBorder, position) < text.indexOf(leftBorder,
					position))
				throw new ParsingException("Unmatched " + rightBorder + " in "
						+ StringUtils.substring(text, position, position + 100));

			StringBuilder dotted = new StringBuilder(text);

			int start = dotted.indexOf(leftBorder, position);
			int end = dotted.indexOf(rightBorder, position);

			if (end == -1)
				throw new ParsingException("Article has unmatched '{' in text");
			if (end < start)
				throw new ParsingException("Article has unmatched '}' in text");

			while (dotted.indexOf(leftBorder, start + 1) != -1
					&& dotted.indexOf(leftBorder, start + 1) < end) {
				int included = start;
				while (dotted.indexOf(leftBorder, included + 1) != -1
						&& dotted.indexOf(leftBorder, included + 1) < end) {
					included = dotted.indexOf(leftBorder, included + 1);
				}

				dotted.replace(
						included,
						end + rightBorder.length(),
						StringUtils.repeat(".", end + rightBorder.length()
								- included));

				end = dotted.indexOf(rightBorder, end);

				if (end == -1)
					throw new ParsingException("Unmatched '{' somewhere in "
							+ StringUtils.substring(text, start, start + 100));
			}

			listener.onOutside(position, start);

			if (start + leftBorder.length() > end)
				throw new ParsingException("Unmatched '}'");

			listener.onInside(start + leftBorder.length(), end);

			position = end + rightBorder.length();
		}

		listener.onOutside(position, text.length());
	}

	protected static int indexOf(CharSequence input, Pattern[] patterns,
			int start) {
		int result = Integer.MAX_VALUE;
		for (Pattern pattern : patterns) {
			Matcher matcher = pattern.matcher(input);
			boolean find = matcher.find(start);
			if (find) {
				int pos = matcher.start();
				if (pos < result)
					result = pos;
			}
		}
		return result != Integer.MAX_VALUE ? result : -1;
	}

	protected static String removeMatched(String text, String beginning,
			String ending) {
		while (text.indexOf(beginning) != -1) {
			int st = text.indexOf(beginning);
			int end = text.indexOf(ending);

			// if several of them inside one
			while (text.indexOf(beginning, st + 1) != -1
					&& text.indexOf(beginning, st + 1) < end)
				st = text.indexOf(beginning, st + 1);

			if (end == -1)
				throw new ParsingException("nonmatched " + beginning
						+ " after '" + StringUtils.mid(text, st, 250) + "...'");

			if (end < st)
				throw new ParsingException("nonmatched "
						+ ending
						+ " before '..."
						+ StringUtils.left(
								StringUtils.substring(text, 0, end + 1), 250)
						+ "'");

			String p1 = text.substring(0, st);
			String p2 = text.substring(st, end + ending.length());
			String p3 = text.substring(end + ending.length());

			text = p1 + StringUtils.repeat(".", p2.length()) + p3;
		}
		return text;
	}

	protected static int tokenAt(CharSequence input, Pattern[] patterns,
			int start) {
		for (int i = 0; i < patterns.length; i++) {
			final Pattern pattern = patterns[i];
			final Matcher matcher = pattern.matcher(input);
			final boolean find = matcher.find(start);
			if (find) {
				if (start == matcher.start())
					return i;
			}
		}
		return -1;
	}

	private void addSection(List<Content> result, Stack<Section> stack,
			Section section) {
		while (!stack.isEmpty()
				&& stack.peek().getLevel() >= section.getLevel()) {
			stack.pop();
		}

		if (stack.isEmpty()) {
			stack.push(section);
			result.add(section);
		} else {
			stack.peek().getContent().getChildren().add(section);
		}
	}

	protected abstract ArticleFragment newArticleFragment(List<Content> contents);

	protected abstract Math newMath(String escapedText);

	protected abstract NoWiki newNoWiki(String escapedText);

	protected abstract Parameter newParameter(final Content value);

	protected abstract Parameter newParameter(final Content paramName,
			Content value);

	protected abstract Section newSection(int level, Content header,
			Text afterHeaderSpaces, Content content);

	protected abstract Table newTable(Content tableAttributes,
			List<Content> tableRows, Text tableTail);

	protected abstract TableCell newTableCell(Content border,
			Content cellAttributes, Content cellAttributesBorder,
			Content content);

	protected abstract TableColumnBreak newTableColumnBreak(
			final Content border, final Content attributes, Text tail);

	protected abstract Template newTemplate(final Content name,
			List<Parameter> parameters);

	protected abstract Text newText(String text);

	public ArticleFragment parse(String text) {
		ProcessObject processObject = processEscapes(text);
		Content content = parseWiki(processObject);

		if (content instanceof ArticleFragment)
			return (ArticleFragment) content;

		ArticleFragment articleFragment = new ArticleFragment(
				Collections.singletonList(content));

		return articleFragment;
	}

	protected Content parseFigureBrackets(ProcessObject processObject) {
		if (processObject.workString.length() == 0) {
			return newText("{}");
		}

		if ('{' == processObject.workString.charAt(0)
				&& '{' != processObject.workString.charAt(1)
				&& '}' == processObject.workString
						.charAt(processObject.workString.length() - 1)) {
			ProcessObject subObject = processObject.subObject(1,
					processObject.workString.length() - 1);
			return parseTempate(subObject);
		}

		// if ('|' == processObject.workString.charAt(0)
		// && '|' == processObject.workString
		// .charAt(processObject.workString.length() - 1)) {
		// ProcessObject subObject = processObject.subObject(1,
		// processObject.workString.length() - 1);
		// return parseSimpleTable(subObject);
		// }
		//
		// if ('{' == processObject.workString.charAt(0))
		// throw new UnsupportedOperationException(processObject.toString());

		// as text :(
		Content asText = parseWiki(processObject);
		if (asText instanceof ArticleFragment) {
			ArticleFragment articleFragment = (ArticleFragment) asText;
			articleFragment.getChildren().add(0, newText("{"));
			articleFragment.getChildren().add(newText("}"));
			return articleFragment;
		} else {
			List<Content> result = new ArrayList<Content>();
			result.add(newText("{"));
			result.add(parseWiki(processObject));
			result.add(newText("}"));
			return newArticleFragment(result);
		}
	}

	protected Content parseHeaders(ProcessObject processObject) {
		List<Content> result = new ArrayList<Content>();
		// all templates are already replaces by "..."
		String workString = processObject.workString;

		if (!patternHeader.matcher(workString).find())
			return processObject.toContent();

		Stack<Section> sections = new Stack<Section>();

		int prevHeaderStart = 0;
		int prevHeaderEnd = 0;
		int prevLevel = -1;
		String prevAfterHeaderSpaces = null;

		Matcher matcher = patternHeader.matcher(workString);
		while (matcher.find(prevHeaderEnd)) {
			final int start = matcher.start();
			final int end = matcher.end();

			String header = matcher.group(1);

			int level = 0;
			while (header.startsWith("=") && header.endsWith("=")) {
				level++;
				header = header.substring(1, header.length() - 1);
			}

			String afterHeaderSpaces = matcher.group(2);
			afterHeaderSpaces = afterHeaderSpaces == null ? StringUtils.EMPTY
					: afterHeaderSpaces;

			if (prevLevel == -1) {
				if (start != 0) {
					// add all no-section content to result
					ProcessObject before = processObject.subObject(0, start);
					Content beforeContent = parseWiki(before);
					if (beforeContent instanceof ArticleFragment) {
						result.addAll(((ArticleFragment) beforeContent)
								.getChildren());
					} else {
						result.add(beforeContent);
					}
				}
			} else {
				// construct prev section

				ProcessObject prevHeaderObject = processObject.subObject(
						prevHeaderStart + prevLevel, prevHeaderEnd - prevLevel
								- prevAfterHeaderSpaces.length());

				ProcessObject prevSectionContentObject = processObject
						.subObject(prevHeaderEnd, start);

				Section prevSection = newSection(prevLevel,
						parseWiki(prevHeaderObject),
						newText(prevAfterHeaderSpaces),
						parseWiki(prevSectionContentObject));

				addSection(result, sections, prevSection);
			}

			prevHeaderStart = start;
			prevHeaderEnd = end;
			prevLevel = level;
			prevAfterHeaderSpaces = afterHeaderSpaces;
		}

		{
			ProcessObject prevHeaderObject = processObject.subObject(
					prevHeaderStart + prevLevel, prevHeaderEnd - prevLevel
							- prevAfterHeaderSpaces.length());

			ProcessObject prevSectionContentObject = processObject.subObject(
					prevHeaderEnd, processObject.workString.length());

			Section prevSection = newSection(prevLevel,
					parseWiki(prevHeaderObject),
					newText(prevAfterHeaderSpaces),
					parseWiki(prevSectionContentObject));

			addSection(result, sections, prevSection);
		}

		return newArticleFragment(result);
	}

	protected Parameter parseParameter(ProcessObject processObject) {
		String dottedText = removeMatched(processObject.workString, "[", "]");
		dottedText = removeMatched(dottedText, "{", "}");

		int pos = dottedText.indexOf('=');
		if (pos == -1) {
			return newParameter(parseWiki(processObject));
		}

		ProcessObject paramName = processObject.subObject(0, pos);
		ProcessObject paramValue = processObject.subObject(pos + 1,
				processObject.workString.length());

		if (!paramName.byPosition.isEmpty())
			throw new ParsingException(
					"Parameter name can't be complex content");

		return newParameter(parseWiki(paramName), parseWiki(paramValue));
	}

	//
	// protected Table parseSimpleTable(ProcessObject processObject) {
	// String dottedText = removeMatched(processObject.workString, "[", "]");
	// dottedText = removeMatched(dottedText, "{", "}");
	// dottedText = removeMatched(dottedText, "<", ">");
	//
	// dottedText = dottedText.replaceAll("[\\u2500-\\u257F]", ".");
	//
	// String tableTail = StringUtils.EMPTY;
	// // while (dottedText.endsWith(" ")) {
	// // tail += " ";
	// // dottedText = dottedText.substring(0, dottedText.length() - 1);
	// // }
	//
	// Content tableAttributes = null;
	// List<Content> tableContent = new ArrayList<Content>();
	//
	// try {
	// TableLexer tableLexer = new TableLexer(new ANTLRStringStream(
	// dottedText));
	//
	// CommonTokenStream tokens = new CommonTokenStream();
	// tokens.setTokenSource(tableLexer);
	//
	// TableParser tableParser = new TableParser(tokens);
	// table_return table_return = tableParser.table();
	// CommonTree commonTree = (CommonTree) table_return.getTree();
	//
	// int position = 0;
	// StringBuilder assertString = new StringBuilder();
	// for (int i = 0; i < commonTree.getChildCount(); i++) {
	// Tree tree = commonTree.getChild(i);
	// switch (tree.getType()) {
	// case TableLexer.TABLEATTRIBUTES: {
	// String strTableAttributes = tree.getText();
	// int newPosition = position + strTableAttributes.length();
	// ProcessObject subObject = processObject.subObject(position,
	// newPosition);
	//
	// tableAttributes = subObject.toContent();
	//
	// position = newPosition;
	// assertString.append(tree.getText());
	// }
	// break;
	// case TableLexer.CELL: {
	// ProcessObject borderObject = null;
	// ProcessObject cellAttributesObject = null;
	// ProcessObject cellAttributesBorderObject = null;
	// ProcessObject contentObject = null;
	//
	// for (int k = 0; k < tree.getChildCount(); k++) {
	// Tree child = tree.getChild(k);
	// switch (child.getType()) {
	// case TableLexer.CELLBORDER: {
	// String strBorder = child.getText();
	// int newPosition = position + strBorder.length();
	// borderObject = processObject.subObject(position,
	// newPosition);
	// position = newPosition;
	// assertString.append(strBorder);
	// }
	// break;
	// case TableLexer.CELLATTRIBUTES: {
	// String strAttributes = child.getText();
	// int newPosition = position + strAttributes.length();
	// cellAttributesObject = processObject.subObject(
	// position, newPosition);
	// position = newPosition;
	// assertString.append(strAttributes);
	// }
	// break;
	// case TableLexer.CELLATTRIBUTESBORDER: {
	// String strAttributesBorder = child.getText();
	// int newPosition = position
	// + strAttributesBorder.length();
	// cellAttributesBorderObject = processObject
	// .subObject(position, newPosition);
	// position = newPosition;
	// assertString.append(strAttributesBorder);
	//
	// }
	// break;
	// case TableLexer.CELLCHILDREN: {
	// String strChildren = child.getText();
	// int newPosition = position + strChildren.length();
	// contentObject = processObject.subObject(position,
	// newPosition);
	// position = newPosition;
	// assertString.append(strChildren);
	// }
	// break;
	// default:
	// throw new UnsupportedOperationException(child
	// .getType()
	// + ": " + child.getText());
	// }
	// }
	//
	// TableCell tableCell = parseTableCell(borderObject,
	// cellAttributesObject, cellAttributesBorderObject,
	// contentObject);
	// tableContent.add(tableCell);
	// }
	// break;
	// case TableLexer.COLUMNBREAK: {
	// ProcessObject borderObject = null;
	// ProcessObject attributesObject = null;
	// ProcessObject columnBreakTail = null;
	//
	// for (int k = 0; k < tree.getChildCount(); k++) {
	// Tree child = tree.getChild(k);
	// switch (child.getType()) {
	// case TableLexer.COLUMNBREAKBORDER: {
	// String strBorder = child.getText();
	// int newPosition = position + strBorder.length();
	// borderObject = processObject.subObject(position,
	// newPosition);
	// position = newPosition;
	// }
	// break;
	// case TableLexer.COLUMNBREAKATTRIBUTES: {
	// String strAttributes = child.getText();
	// int newPosition = position + strAttributes.length();
	// attributesObject = processObject.subObject(
	// position, newPosition);
	// position = newPosition;
	// }
	// break;
	// case TableLexer.COLUMNBREAKTAIL: {
	// String strTail = child.getText();
	//
	// int newPosition = position + strTail.length();
	// columnBreakTail = processObject.subObject(position,
	// newPosition);
	// position = newPosition;
	// }
	// break;
	// default:
	// throw new UnsupportedOperationException(child
	// .getType()
	// + ": " + child.getText());
	// }
	//
	// }
	//
	// tableContent.add(parseTableColumnBreak(borderObject,
	// attributesObject, columnBreakTail));
	//
	// assertString.append(tree.getText());
	// }
	// break;
	// case TableLexer.TABLETAIL: {
	// String dottedTail = tree.getText();
	// if (!StringUtils.isEmpty(dottedTail)) {
	// if (!StringUtils.containsOnly(dottedTail, " \t\r\n"))
	// throw new AssertionError(dottedTail);
	//
	// tableTail = dottedTail;
	//
	// assertString.append(dottedTail);
	// }
	// }
	// break;
	// default:
	// throw new UnsupportedOperationException(tree.getType()
	// + ": " + tree.getText());
	// }
	//
	// if (!assertString.toString().equals(
	// dottedText.substring(0, assertString.length())))
	// throw new AssertionError();
	//
	// }
	//
	// if (!assertString.toString().equals(dottedText))
	// throw new AssertionError();
	//
	// } catch (RecognitionException exc) {
	// throw new ParsingException("Unable to parse table because of: '"
	// + exc.getMessage() + "'; «" + dottedText + "»", exc);
	// } catch (ParsingException exc) {
	// throw new ParsingException("Unable to parse table because of: '"
	// + exc.getMessage() + "'; «" + dottedText + "»", exc
	// .getCause());
	// }
	//
	// return newTable(tableAttributes, tableContent, newText(tableTail));
	// }

	protected TableCell parseTableCell(ProcessObject borderObject,
			ProcessObject cellAttributesObject,
			ProcessObject cellAttributesBorderObject,
			ProcessObject contentObject) {
		final Content border = borderObject.toContent();
		final Content cellAttributes = cellAttributesObject == null ? null
				: cellAttributesObject.toContent();
		final Content cellAttributesBorder = cellAttributesBorderObject == null ? null
				: cellAttributesBorderObject.toContent();
		final Content content = contentObject != null ? contentObject
				.toContent() : null;

		return newTableCell(border, cellAttributes, cellAttributesBorder,
				content);
	}

	protected TableColumnBreak parseTableColumnBreak(
			ProcessObject borderObject, ProcessObject attributesObject,
			ProcessObject tailObject) {
		final Content border = borderObject.toContent();
		final Content attributes = attributesObject == null ? null
				: attributesObject.toContent();

		final Text tail;
		if (tailObject != null) {
			if (!StringUtils.containsOnly(tailObject.workString, " \t\r\n"))
				throw new AssertionError(tailObject.workString);

			tail = newText(tailObject.workString);
		} else {
			tail = null;
		}

		return newTableColumnBreak(border, attributes, tail);
	}

	protected Template parseTempate(ProcessObject processObject) {
		String dottedText = removeMatched(processObject.workString, "[", "]");
		dottedText = removeMatched(dottedText, "{", "}");

		if (processObject.workString.length() != dottedText.length())
			throw new AssertionError();

		final List<ProcessObject> tokens = new ArrayList<ProcessObject>();

		if (dottedText.indexOf("|") == -1) {
			tokens.add(processObject);
		} else {
			int prevPosition = 0;
			while (dottedText.indexOf("|", prevPosition + 1) != -1) {
				int position = dottedText.indexOf("|", prevPosition + 1);

				ProcessObject token = processObject.subObject(prevPosition,
						position);
				tokens.add(token);

				prevPosition = position + 1;
			}

			if (prevPosition <= processObject.workString.length()) {
				ProcessObject token = processObject.subObject(prevPosition,
						processObject.workString.length());
				tokens.add(token);
			}
		}

		Content name = parseWiki(tokens.get(0));
		List<Parameter> parameters = new ArrayList<Parameter>(tokens.size() - 1);
		for (int i = 1; i < tokens.size(); i++) {
			ProcessObject token = tokens.get(i);
			Parameter parameter = parseParameter(token);
			parameters.add(parameter);
		}

		return newTemplate(name, parameters);
	}

	protected Content parseWiki(ProcessObject processObject) {
		processObject = processFigureBrackets(processObject);
		return parseHeaders(processObject);
	}

	protected ProcessObject processEscapes(String originalText) {
		final StringBuilder stringBuilder = new StringBuilder();
		final LinkedHashMap<Integer, Content> byPosition = new LinkedHashMap<Integer, Content>();

		StringBuilder text = new StringBuilder(originalText);

		int position = 0;
		final Pattern[] patterns = new Pattern[] {
				Pattern.compile(Pattern.quote("<!--")),
				Pattern.compile(Pattern.quote("<nowiki>")),
				Pattern.compile(Pattern.quote("<math>")) };
		while (indexOf(text, patterns, position) != -1) {
			int newPosition = indexOf(text, patterns, position);
			int pattern = tokenAt(text, patterns, newPosition);

			if (newPosition != position) {
				stringBuilder.append(text.substring(position, newPosition));
			}

			if (pattern == 0) {
				Matcher matcher = Pattern.compile(Pattern.quote("-->"))
						.matcher(text);
				if (!matcher.find(newPosition)) {
					throw new ParsingException("Unmatched <!-- in article text");
				}

				int end = matcher.start();
				final int closingLength = matcher.end() - matcher.start();

				String escaped = text.substring(newPosition + "<!--".length(),
						end);
				byPosition.put(stringBuilder.length(), new Comment(escaped));
				stringBuilder.append(".");

				text.replace(
						newPosition,
						end + closingLength,
						StringUtils.repeat(".", end - newPosition
								+ closingLength));

				position = end + closingLength;
			}

			if (pattern == 1) {
				Matcher matcher = Pattern.compile(Pattern.quote("</nowiki>"))
						.matcher(text);
				if (!matcher.find(newPosition)) {
					throw new ParsingException(
							"Unmatched <nowiki> in article text");
				}

				int end = matcher.start();
				final int closingLength = matcher.end() - matcher.start();

				String escaped = text.substring(
						newPosition + "<nowiki>".length(), end);
				byPosition.put(stringBuilder.length(), newNoWiki(escaped));
				stringBuilder.append(".");

				text.replace(
						newPosition,
						end + closingLength,
						StringUtils.repeat(".", end - newPosition
								+ closingLength));

				position = end + closingLength;
			}

			if (pattern == 2) {
				Matcher matcher = Pattern.compile(Pattern.quote("</math>"))
						.matcher(text);
				if (!matcher.find(newPosition)) {
					throw new ParsingException(
							"Unmatched <math> in article text");
				}
				int end = matcher.start();
				final int closingLength = matcher.end() - matcher.start();

				String escaped = text.substring(
						newPosition + "<math>".length(), end);
				byPosition.put(stringBuilder.length(), newMath(escaped));
				stringBuilder.append(".");

				text.replace(
						newPosition,
						end + closingLength,
						StringUtils.repeat(".", end - newPosition
								+ closingLength));

				position = end + closingLength;
			}
		}

		if (position != text.length()) {
			stringBuilder.append(text.substring(position, text.length()));
		}

		return new ProcessObject(stringBuilder.toString(), byPosition);
	}

	protected ProcessObject processFigureBrackets(
			final ProcessObject processObject) {
		final StringBuilder stringBuilder = new StringBuilder(
				processObject.workString.length());
		final LinkedHashMap<Integer, Content> byPosition = new LinkedHashMap<Integer, Content>();

		extractByBorder(processObject.workString, "{", "}",
				new BorderListener() {

					public void onInside(int startIncluded, int endExcluded) {
						ProcessObject subObject = processObject.subObject(
								startIncluded, endExcluded);
						byPosition.put(stringBuilder.length(),
								parseFigureBrackets(subObject));
						stringBuilder.append(".");
					}

					public void onOutside(int startIncluded, int endExcluded) {
						processObject.copySubObjectInto(startIncluded,
								endExcluded, stringBuilder, byPosition);
					}

				});
		return new ProcessObject(stringBuilder.toString(), byPosition);
	}
}
