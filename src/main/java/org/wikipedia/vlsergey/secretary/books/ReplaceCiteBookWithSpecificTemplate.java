/*
 * Copyright 2001-2008 Fizteh-Center Lab., MIPT, Russia
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
 * Created on 10.04.2008
 */
package org.wikipedia.vlsergey.secretary.books;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.dom.Text;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespaces;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;
import org.wikipedia.vlsergey.secretary.webcite.RefAwareParser;

public class ReplaceCiteBookWithSpecificTemplate implements Runnable {

	private static class ReplaceByISBN {

		private final boolean addAuthorPlaceholder;

		private final String edition;

		private final String isbn;

		private final int pages;

		private final String templateName;

		private final String том;

		public ReplaceByISBN(String templateName, String edition, boolean addAuthorPlaceholder, int pages, String том,
				String isbn) {
			this.templateName = templateName;
			this.edition = edition;
			this.addAuthorPlaceholder = addAuthorPlaceholder;
			this.pages = pages;
			this.том = том;
			this.isbn = isbn;
		}
	}

	private static final Log log = LogFactory.getLog(ReplaceCiteBookWithSpecificTemplate.class);

	private static List<ReplaceByISBN> replaceByISBN;

	static {
		replaceByISBN = new ArrayList<ReplaceByISBN>();

		/* G */
		replaceByISBN.add(new ReplaceByISBN("Книга:Gajl T.: Polish Armorial Middle Ages to 20th Century", null, true,
				-1, null, "978-83-60597-10-1"));

		/* А */
		replaceByISBN.add(new ReplaceByISBN("Книга:Атлас пресмыкающихся Северной Евразии", null, false, 232, null,
				"5-98092-007-2"));

		/* Б */
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Биографии российских генералиссимусов и генерал-фельдмаршалов",
		// "1-2", true, 620, null, "5-7158-0002-1"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Биографии российских генералиссимусов и генерал-фельдмаршалов",
		// "3-4", true, 640, null, "5-7158-0010-2"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Бишоп К.: Подводные лодки Кригсмарине", "2007", true,
		// 192, null, "978-5-699-22106-6"));

		// replaceByISBN.add(new ReplaceByISBN("Книга:Бог войны Третьего рейха",
		// null, false, 576, null, "5-17015-302-3"));

		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Бурлаков А. В.:Туристские маршруты Гатчинского района. Южное направление",
		// "2003", true, 120, null, "5-94331-037-1"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Витязева В.А., Кириков Б.М.: Ленинград: Путеводитель",
		// "1988", true, 366, null, "5-289-00492-0"));

		/* Г */
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Городские имена сегодня и вчера", "1997", true, 288,
		// null, "5-86038-023-2"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Городские имена сегодня и вчера", "1997", true, 288,
		// null, "5-86038-023-2"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Горбачевич К.С., Хабло Е.П.: Почему так названы?",
		// "1996", true, 359, null, "5-7711-0002-1"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Горбачевич К.С., Хабло Е.П.: Почему так названы?",
		// "2002", true, 353, null, "5-7711-0019-6"));

		replaceByISBN.add(new ReplaceByISBN("Книга:Город Салават", null, false, 80, null, "5-87308-120-4"));

		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Густерин П.В.: Города Арабского Востока", "2007", true,
		// 352, null, "978-5-478-00729-4"));
		//
		// replaceByISBN.add(new ReplaceByISBN("Книга:Грюнерт Г.: Грибы",
		// "2001",
		// true, 288, null, "5-17-006175-7"));

		/* Д */
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Дёниц К.: Десять лет и двадцать дней", "2004", true,
		// 495, null, "5-9524-1356-0"));

		/* Д */
		replaceByISBN.add(new ReplaceByISBN("Книга:Железнодорожный транспорт: Энциклопедия", null, false, 599, null,
				"5-85270-115-7"));

		/* З */
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Исаченко В.Г.: Зодчие Санкт-Петербурга", "XVIII", true,
		// 1021, null, "5-289-01585-X"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Исаченко В.Г.: Зодчие Санкт-Петербурга", "XIX", true,
		// 1070, null, "5-289-01586-8"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Исаченко В.Г.: Зодчие Санкт-Петербурга", "XX", true,
		// 720, null, "5-289-01928-6"));

		/* И */
		// replaceByISBN.add(new
		// ReplaceByISBN("Книга:История Республики Молдова",
		// "2002", true, 360, null, "9975-9719-5-4"));

		/* К */
		replaceByISBN.add(new ReplaceByISBN("Книга:Казахская ССР: краткая энциклопедия", null, false, 607, "2",
				"5-89800-002-X"));

		replaceByISBN.add(new ReplaceByISBN("Книга:Казахская ССР: краткая энциклопедия", null, false, 598, "3",
				"5-89900-006-2"));

		replaceByISBN.add(new ReplaceByISBN("Книга:Казахская ССР: краткая энциклопедия", null, false, 685, "4",
				"5-89800-023-2"));

		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Как создавался атомный подводный флот Советского Союза",
		// "2004", true, 544, null, "5-17-025848-8"));

		/* Л */
		// replaceByISBN.add(new ReplaceByISBN("Книга:Лессо Т.: Грибы", "2003",
		// true, 304, null, "5-17-020333-0"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Ландау Л.Д., Лившиц Е.М.: Квантовая механика", "1989",
		// true, 768, null, "5-02-014421-5"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Ландау Л.Д., Лившиц Е.М.: Механика", "1988", true, 215,
		// null, "5-02-013850-9"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Ландау Л.Д., Лившиц Е.М.: Теория поля", "1988", true,
		// 512, null, "5-02-014420-7"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Морозов М.Э., Кулагин К.Л.: Щуки", "2008", true, 176,
		// null, "978-5-699-25285-5"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Мультатули П.В.: Господь да благословит решение мое…",
		// "2002", true, -1, null, "978-5-7373-0171-2"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Никитенко Г.Ю., Соболь В.Д.:Василеостровский район",
		// "2002", true, 534, null, "5-89771-030-9"));

		// XXX: некорректный шаблон!
		replaceByISBN.add(new ReplaceByISBN("Книга:Определитель насекомых дв-3-1", null, false, 572, null,
				"5-02-025623-4"));

		/* П */

		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Петербургский метрополитен: от идеи до воплощения",
		// "2005", true, 160, null, "5-902671-21-3"));
		//
		// replaceByISBN.add(new
		// ReplaceByISBN("Книга:Пиллар Л.: Подводная война",
		// "2007", true, 432, null, "978-5-9524-2994-9"));

		replaceByISBN.add(new ReplaceByISBN("Книга:Пряно-ароматические и пряно-вкусовые растения", null, false, 304,
				null, "5-12-000483-0"));

		replaceByISBN.add(new ReplaceByISBN("Книга:Пятиязычный словарь названий животных. Насекомые", null, false, 560,
				null, "5-88721-162-8"));

		replaceByISBN.add(new ReplaceByISBN("Книга:Пятиязычный словарь названий животных. Птицы", null, false, 845,
				null, "5-200-00643-0"));
		/* Р */

		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Реданский В.Г.: Во льдах и подо льдами", "2004", true,
		// 480, null, "5-9533-0192-8"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Реданский В.Г.: Во льдах и подо льдами", "2004", true,
		// 480, null, "5-9533-0192-8"));

		replaceByISBN.add(new ReplaceByISBN("Книга:Российский парусный флот", "1", false, 312, null, "5-203-01788-3"));

		replaceByISBN.add(new ReplaceByISBN("Книга:Российский парусный флот", "2", false, 480, null, "5-203-01789-1"));

		/* C */
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Справочник-определитель: Грибы", "2002", true, 416,
		// null, "985-13-0913-3"));
		//
		// replaceByISBN.add(new ReplaceByISBN("Стати В.: История Молдовы",
		// "2002", true, 480, null, "9975-9504-1-8"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Роувер Ю.: Субмарины, несущие смерть", "2004", true,
		// 416, null, "5-9524-1237-8"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Сунгуров А.Ю.: Этюды политической жизни Ленинграда — Петербурга",
		// "1996", true, 196, null, "5-87427-008-6"));

		/* Т */
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Тарас A.E.: Вторая мировая война на море", "2003", true,
		// 640, null, "985-13-1707-1"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Толковый словарь спортивных терминов", "1993", true,
		// 352, null, "5-278-00558-0"));
		//
		// replaceByISBN.add(new ReplaceByISBN(
		// "Книга:Толковый словарь спортивных терминов", "2001", true,
		// 480, null, "5-8134-0047-8"));

		/* У */
		// replaceByISBN.add(new ReplaceByISBN("Книга:Уду Ж.: Грибы", "2003",
		// true, 191, null, "5-271-05827-1"));
		//
		// replaceByISBN.add(new ReplaceByISBN("Книга:Уду Ж.: Грибы", "2003",
		// true, 191, null, "5-17-017165-X"));

		replaceByISBN.add(new ReplaceByISBN("Книга:Универсальная энциклопедия лекарственных растений", null, false,
				656, null, "5-88215-969-5"));

		/* Ш */
		replaceByISBN.add(new ReplaceByISBN("Книга:Шахматы. Энциклопедический словарь", null, false, 621, null,
				"5-85270-005-3"));

		/* Э */
		replaceByISBN.add(new ReplaceByISBN("Книга:Энциклопедия советских надводных кораблей", null, false, 640, null,
				"5-89173-178-9"));

		replaceByISBN.add(new ReplaceByISBN(
				"Книга:Энциклопедический биографический словарь музыкантов-исполнителей на духовых инструментах",
				"1995", false, 358, null, "5-88123-007-8"));

		// replaceByISBN.add(new ReplaceByISBN("Книга:Эски в бою", "2008", true,
		// 176, null, "978-5-699-25627-3"));
	}

	private MediaWikiBot mediaWikiBot;

	private RefAwareParser refAwareParser;

	private WikiCache wikiCache;

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public RefAwareParser getRefAwareParser() {
		return refAwareParser;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	private void process(Revision revision) throws Exception {
		ArticleFragment fragment = getRefAwareParser().parse(revision.getXml());
		String source = fragment.toWiki(false);

		final LinkedHashMap<String, List<Template>> allTemplates = fragment.getAllTemplates();

		if (allTemplates.containsKey("книга")) {
			for (Template template : allTemplates.get("книга")) {
				заменитьНаИсточникШаблонКнига(template);
			}
		}

		if (allTemplates.containsKey("cite book")) {
			for (Template template : allTemplates.get("cite book")) {
				заменитьНаИсточникШаблонCiteBook(template);
			}
		}

		String result = fragment.toWiki(false);

		if (!StringUtils.equals(source, result)) {
			mediaWikiBot.writeContent(revision.getPage(), revision, result, "Replace with book template(s)", true);
		}
	}

	@Override
	public void run() {
		for (Revision revision : wikiCache.queryContentByPagesAndRevisions(mediaWikiBot
				.queryPagesWithRevisionByEmbeddedIn("Template:Книга", new int[] { Namespaces.MAIN },
						new RevisionPropery[] { RevisionPropery.IDS }))) {
			try {
				process(revision);
			} catch (Exception exc) {
				log.error("Unable to process with " + revision + ": " + exc, exc);
			}
		}
		for (Revision revision : wikiCache.queryContentByPagesAndRevisions(mediaWikiBot
				.queryPagesWithRevisionByEmbeddedIn("Template:Cite book", new int[] { Namespaces.MAIN },
						new RevisionPropery[] { RevisionPropery.IDS }))) {
			try {
				process(revision);
			} catch (Exception exc) {
				log.error("Unable to process with " + revision + ": " + exc, exc);
			}
		}
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setRefAwareParser(RefAwareParser refAwareParser) {
		this.refAwareParser = refAwareParser;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}

	private void добавитьПараметрИздания(Template шаблон, String параметрИздания) {
		шаблон.getParameters().add(new TemplatePart(null, new Text(параметрИздания)));
	}

	private void добавитьЧастьИСтраницы(Template template, boolean addAuthorParameterPlace, String part, String link,
			String pages, String том) {
		if (addAuthorParameterPlace && (part != null || link != null || pages != null)) {
			template.getParameters().add(new TemplatePart(null, new Text("")));
		}

		if (part != null) {
			template.setParameterValue("часть", new Text(part));
		}

		if (link != null) {
			template.setParameterValue("ссылка", new Text(link));
		}

		if (pages != null) {
			template.setParameterValue("страницы", new Text(pages));
		}

		if (том != null) {
			template.setParameterValue("том", new Text(том));
		}

	}

	private void заменитьНаИсточникЧерезISBN(Template шаблон, String isbn, String part, String link, String pages) {

		for (ReplaceByISBN replace : replaceByISBN) {
			if (StringUtils.equalsIgnoreCase(replace.isbn, isbn)) {

				if (StringUtils.equalsIgnoreCase(pages, "" + replace.pages)) {
					pages = null;
				}

				if (link != null && link.contains("books.ru"))
					link = null;

				шаблон.setTitle(new Text(replace.templateName));

				шаблон.getParameters().clear();
				if (replace.edition != null)
					добавитьПараметрИздания(шаблон, replace.edition);

				добавитьЧастьИСтраницы(шаблон, replace.addAuthorPlaceholder, part, link, pages, replace.том);
			}
		}

	}

	private void заменитьНаИсточникЧерезЗаглавиеИГод(Template шаблон, String title, String year, String part,
			String link, String pages) {

		title = title.toLowerCase();
		title = title.replace(".", " ").trim();
		while (title.contains("  "))
			title = title.replace("  ", " ");

		if ((title.equals("государственный эрмитаж западноевропейская живопись каталог") && year.equals("1981"))
				|| (title.equals("государственный эрмитаж западноевропейская живопись") && year.equals("1981"))) {
			шаблон.setTitle(new Text("Книга:Государственный Эрмитаж. Западноевропейская живопись"));
			шаблон.getParameters().clear();
			добавитьПараметрИздания(шаблон, "1981");
			добавитьЧастьИСтраницы(шаблон, false, part, link, pages, null);
		}
	}

	private void заменитьНаИсточникШаблонCiteBook(Template шаблон) {
		String isbn1 = StringUtils.trimToEmpty(шаблон.getParameterValue("id") != null ? шаблон.getParameterValue("id")
				.toWiki(true) : null);
		if (isbn1 != null && isbn1.toLowerCase().startsWith("isbn"))
			isbn1 = StringUtils.trimToEmpty(isbn1.substring(4));

		String isbn2 = StringUtils.trimToEmpty(шаблон.getParameterValue("isbn") != null ? шаблон.getParameterValue(
				"isbn").toWiki(true) : null);
		if (isbn2 != null && isbn2.toLowerCase().startsWith("isbn"))
			isbn2 = StringUtils.trimToEmpty(isbn2.substring(4));

		String isbn = StringUtils.trimToNull(isbn1 + isbn2);

		if (isbn == null)
			return;

		String pages1 = StringUtils.trimToEmpty(шаблон.getParameterValue("pages") != null ? шаблон.getParameterValue(
				"pages").toWiki(true) : null);
		String pages2 = StringUtils.trimToEmpty(шаблон.getParameterValue("страницы") != null ? шаблон
				.getParameterValue("страницы").toWiki(true) : null);
		String pages = StringUtils.trimToNull(pages1 + pages2);

		String part1 = StringUtils.trimToEmpty(шаблон.getParameterValue("chapter") != null ? шаблон.getParameterValue(
				"chapter").toWiki(true) : null);
		String part2 = StringUtils.trimToEmpty(шаблон.getParameterValue("часть") != null ? шаблон.getParameterValue(
				"часть").toWiki(true) : null);
		String part = StringUtils.trimToNull(part1 + part2);

		заменитьНаИсточникЧерезISBN(шаблон, isbn, part, null, pages);
	}

	private void заменитьНаИсточникШаблонКнига(Template шаблон) {

		String pages = StringUtils.trimToNull(шаблон.getParameterValue("страницы") != null ? шаблон.getParameterValue(
				"страницы").toWiki(true) : null);

		String part = StringUtils.trimToNull(шаблон.getParameterValue("часть") != null ? шаблон.getParameterValue(
				"часть").toWiki(true) : null);

		String link = StringUtils.trimToNull(шаблон.getParameterValue("ссылка") != null ? шаблон.getParameterValue(
				"ссылка").toWiki(true) : null);

		{
			Content parameterISBN = шаблон.getParameterValue("isbn");
			if (parameterISBN != null && parameterISBN.toWiki(true).trim().length() != 0) {
				String isbn = parameterISBN.toWiki(true).trim();

				заменитьНаИсточникЧерезISBN(шаблон, isbn, part, link, pages);
			}
		}

		{
			Content paramTitle = шаблон.getParameterValue("заглавие");
			Content paramYear = шаблон.getParameterValue("год");
			if (paramTitle != null && paramTitle.toWiki(true).trim().length() != 0 && paramYear != null
					&& paramYear.toWiki(true).trim().length() != 0) {

				String title = paramTitle.toWiki(true).trim();
				String year = paramYear.toWiki(true).trim();

				заменитьНаИсточникЧерезЗаглавиеИГод(шаблон, title, year, part, link, pages);
			}
		}
	};
}
