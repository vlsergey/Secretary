package org.wikipedia.vlsergey.secretary.dom.parser;

import java.io.IOException;

import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

public class XmlParserTest {

	private void test(final String articleName) throws IOException, Exception {
		String data = StringEscapeUtils.unescapeXml(IoUtils.readToString(
				XmlParserTest.class.getResourceAsStream("" + articleName + ".xml.escaped"), "utf-8"));
		ArticleFragment fragment = new XmlParser().parse(data);

		final String reparsed = fragment.toWiki(false).replace("\r", "");
		System.out.println(reparsed);

		String wiki = StringEscapeUtils.unescapeXml(
				IoUtils.readToString(XmlParserTest.class.getResourceAsStream(articleName + ".wiki.escaped"), "utf-8"))
				.replace("\r", "");
		Assert.assertEquals(wiki, reparsed);
	}

	@Test
	public void Бразилия() throws Exception {
		test("Бразилия");
	}

	// @Test
	public void Википедия_К_Удалению() throws Exception {

		String data = StringEscapeUtils.unescapeXml(IoUtils.readToString(
				XmlParserTest.class.getResourceAsStream("Википедия_К_Удалению.xml.escaped"), "utf-8"));
		ArticleFragment fragment = new XmlParser().parse(data);

		System.out.println(fragment.toWiki(false));
	}

	// @Test
	public void Википедия_К_Удалению_1_марта_2012() throws Exception {

		String data = StringEscapeUtils.unescapeXml(IoUtils.readToString(
				XmlParserTest.class.getResourceAsStream("Википедия_К_Удалению_1_марта_2012.xml.escaped"), "utf-8"));
		ArticleFragment fragment = new XmlParser().parse(data);

		System.out.println(fragment.toWiki(false));
	}

	@Test
	public void Данис() throws Exception {
		test("Дания");
	}

	@Test
	public void Соционика() throws Exception {
		test("Соционика");
	}
}
