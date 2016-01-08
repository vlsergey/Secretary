package org.wikipedia.vlsergey.secretary.laws;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.vlsergey.secretary.http.HttpManager;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

public class PageParser {

	private static final String REGEXP_SPACE = "(\\s|\u00a0|&#xA0;)*";

	private static final String REGEXP_SPACE_OR_BR = "(<[bB][rR]\\s*/>|\n|\\s|\u00a0|&#xA0;)*";

	public static void main(String[] args) throws Exception {
		final PageParser pageParser = new PageParser();
		// pageParser.parse("http://" + "docs.kodeks.ru/document/902318605",
		// true);

		String list = IoUtils.readToString(PageParser.class.getResource("text.html").openStream(), "utf-8");
		Pattern pattern = Pattern.compile("<a href=\"/document/([0-9]*)\">");
		Matcher matcher = pattern.matcher(list);
		while (matcher.find()) {
			String number = matcher.group(1);

			// if (pageParser.mediaWikiBot.queryExternalUrlUsage("http",
			// "docs.kodeks.ru/document/" + number, 0)
			// .iterator().hasNext()) {
			// continue;
			// }

			pageParser.parse("http://" + "docs.kodeks.ru/document/" + number, true);
		}
	}

	final HttpManager httpManager;

	final MediaWikiBot mediaWikiBot;

	public PageParser() throws Exception {
		this.httpManager = new HttpManager();
		httpManager.afterPropertiesSet();
		this.mediaWikiBot = new MediaWikiBot();
		mediaWikiBot.setHttpManager(httpManager);
		mediaWikiBot.init();

		mediaWikiBot.setSite(new URI("http://ru.wikisource.org/w"));
		mediaWikiBot.setLogin(System.getProperty("org.wikipedia.vlsergey.bot.login"));
		mediaWikiBot.setPassword(System.getProperty("org.wikipedia.vlsergey.bot.password"));
		mediaWikiBot.login();
	}

	private String addHyperlinks(String wiki) throws ParseException {
		// add hyperlinks
		{
			Pattern pattern = Pattern
					.compile("([^\\|])(Указ|Указа|Указе|Указом) Президента Российской Федерации от ([0-9]+ [а-я]+ [0-9]+) года №&nbsp;([0-9]+) «([^»]*)»");
			Matcher matcher = pattern.matcher(wiki);
			while (matcher.find()) {
				wiki = matcher.replaceFirst("$1[[Указ Президента РФ от " + toDate(matcher.group(3))
						+ " № $4|$2 Президента Российской Федерации от $3 года №&nbsp;$4 «$5»]]");
				matcher = pattern.matcher(wiki);
			}
		}
		{
			Pattern pattern = Pattern
					.compile("([^\\|])(Указ|Указа|Указе|Указом) Президента Российской Федерации от ([0-9]+.[0-9]+.[0-9]+) N ([0-9]+) «([^»]*)»");
			Matcher matcher = pattern.matcher(wiki);
			while (matcher.find()) {
				wiki = matcher.replaceFirst("$1[[Указ Президента РФ от " + matcher.group(3)
						+ " № $4|$2 Президента Российской Федерации от $3 №&nbsp;$4 «$5»]]");
				matcher = pattern.matcher(wiki);
			}
		}
		{
			Pattern pattern = Pattern
					.compile("([^\\|])(Указ|Указа|Указе|Указом) Президента Российской Федерации от ([0-9]+ [а-я]+ [0-9]+) года №&nbsp;([0-9]+)");
			Matcher matcher = pattern.matcher(wiki);
			while (matcher.find()) {
				wiki = matcher.replaceFirst("$1[[Указ Президента РФ от " + toDate(matcher.group(3))
						+ " № $4|$2 Президента Российской Федерации от $3 года №&nbsp;$4]]");
				matcher = pattern.matcher(wiki);
			}
		}

		{
			Pattern pattern = Pattern
					.compile("([^\\|])(постановлением) Правительства Российской Федерации от ([0-9]+ [а-я]+ [0-9]+) года №&nbsp;([0-9]+)");
			Matcher matcher = pattern.matcher(wiki);
			while (matcher.find()) {
				wiki = matcher.replaceFirst("$1[[Постановление Президента РФ от " + toDate(matcher.group(3))
						+ " № $4|$2 Правительства Российской Федерации от $3 года №&nbsp;$4]]");
				matcher = pattern.matcher(wiki);
			}
		}

		{
			Pattern pattern = Pattern
					.compile("статьей ([0-9]+) Федерального закона от ([0-9]+ [а-я]+ [0-9]+) года №&nbsp;([0-9]+-ФЗ) «(.*)»");
			Matcher matcher = pattern.matcher(wiki);
			while (matcher.find()) {
				wiki = matcher.replaceFirst("[[Федеральный закон от " + toDate(matcher.group(2))
						+ " № $3#Статья $1|статьёй $1 Федерального закона от $2 года №&nbsp;$3 «$4»]]");
				matcher = pattern.matcher(wiki);
			}
		}
		{
			Pattern pattern = Pattern
					.compile("([^\\|])статьи ([0-9]+) Федерального закона от ([0-9]+ [а-я]+ [0-9]+) года №&nbsp;([0-9]+-ФЗ) «(.*)»([^\\]]|$)");
			Matcher matcher = pattern.matcher(wiki);
			while (matcher.find()) {
				wiki = matcher.replaceFirst("$1[[Федеральный закон от " + toDate(matcher.group(3))
						+ " № $4#Статья $2|статьи $2 Федерального закона от $3 года №&nbsp;$4 «$5»]]$6");
				matcher = pattern.matcher(wiki);
			}
		}

		{
			Pattern pattern = Pattern
					.compile("федеральными законами от ([0-9]+ [а-я]+ [0-9]+) года №&nbsp;([0-9]+-ФЗ) «(.*)»"
							+ " и от ([0-9]+ [а-я]+ [0-9]+) года №&nbsp;([0-9]+-ФЗ) «(.*)»");
			Matcher matcher = pattern.matcher(wiki);
			while (matcher.find()) {
				wiki = matcher.replaceFirst("федеральными законами " + "[[Федеральный закон от "
						+ toDate(matcher.group(1)) + " №&nbsp;$2|от $1 года №\u00a0$2 «$3»]]" + " и "
						+ "[[Федеральный закон от " + toDate(matcher.group(4)) + " № $5|от $4 года №&nbsp;$5 «$6»]]");
				matcher = pattern.matcher(wiki);
			}
		}

		{
			Pattern pattern = Pattern
					.compile("([^\\|])(Федеральн[а-я]* закон[а-я]*) от ([0-9]+ [а-я]+ [0-9]+) года №&nbsp;([0-9]+-ФЗ) «(.*)»([^\\]]|$)");
			Matcher matcher = pattern.matcher(wiki);
			while (matcher.find()) {
				wiki = matcher.replaceFirst("$1[[Федеральный закон от " + toDate(matcher.group(3))
						+ " № $4|$2 от $3 года №&nbsp;$4 «$5»]]$6");
				matcher = pattern.matcher(wiki);
			}
		}

		{
			Pattern pattern = Pattern
					.compile("статьей ([0-9]+) Федерального конституционного закона от ([0-9]+ [а-я]+ [0-9]+) года №&nbsp;([0-9]+-ФКЗ) «(.*)»");
			Matcher matcher = pattern.matcher(wiki);
			while (matcher.find()) {
				wiki = matcher
						.replaceFirst("[[Федеральный конституционный закон от "
								+ toDate(matcher.group(2))
								+ " № $3#Статья $1|статьёй&nbsp;$1 Федерального конституционного закона от $2 года №&nbsp;$3 «$4»]]");
				matcher = pattern.matcher(wiki);
			}
		}

		// руководствуется Конституцией Российской Федерации
		wiki = wiki.replaceAll("руководствуется Конституцией Российской Федерации",
				"руководствуется [[Конституция Российской Федерации|Конституцией Российской Федерации]]");

		// руководствуется Конституцией Российской Федерации
		wiki = wiki.replaceAll(" статьи ([0-9]+) Конституции Российской Федерации",
				" [[Конституция Российской Федерации#Статья_$1|статьи $1 Конституции Российской Федерации]]");

		// руководствуется Конституцией Российской Федерации
		wiki = wiki.replaceAll("статьями ([0-9]+) и ([0-9]+) Конституции Российской Федерации",
				"статьями [[Конституция Российской Федерации#Статья_$1|$1]] и [[Конституция Российской Федерации#Статья_$2|$2]] "
						+ "[[Конституция Российской Федерации|Конституции Российской Федерации]]");
		return wiki;
	}

	public void parse(String url, boolean save) throws Exception {
		String html = IoUtils.readToString(new URL(url).openStream(), "utf-8");

		while (html.contains("\t") || html.contains("\n\n") || html.contains("\n ") || html.contains(" \n")
				|| html.contains(">\n") || html.contains("\n<")
		// || html.contains("> ") || html.contains(" <")
		) {
			html = StringUtils.replace(html, "\t", "    ");
			html = StringUtils.replace(html, "\n\n", "\n");
			html = StringUtils.replace(html, "\n ", "\n");
			html = StringUtils.replace(html, " \n", "\n");
			html = StringUtils.replace(html, ">\n", ">");
			html = StringUtils.replace(html, "\n<", "<");
			// html = StringUtils.replace(html, "> ", ">");
			// html = StringUtils.replace(html, " <", "<");
		}

		html = html.replaceAll("<B/>", "");
		html = html.replaceAll("</B><B>", "");
		html = html.replaceAll("<B>(" + REGEXP_SPACE_OR_BR + ")</B>", "$1");
		html = html.replaceAll(REGEXP_SPACE_OR_BR + "</div>" + REGEXP_SPACE_OR_BR, "</div>");
		html = html.replaceAll(REGEXP_SPACE_OR_BR + "<div ", "<div ");
		html = html.replaceAll(REGEXP_SPACE_OR_BR + "<h1 ", "<h1 ");
		html = html.replaceAll(REGEXP_SPACE_OR_BR + "<h1>", "<h1>");
		html = html.replaceAll(REGEXP_SPACE_OR_BR + "<P ", "<P ");
		html = html.replaceAll(REGEXP_SPACE_OR_BR + "<p ", "<p ");

		html = html.replaceAll(REGEXP_SPACE + "<br ", "<br ");
		html = html.replaceAll(REGEXP_SPACE + "<BR/>" + REGEXP_SPACE, "<BR/>");
		html = html.replaceAll(REGEXP_SPACE + "<br/>" + REGEXP_SPACE, "<br/>");

		html = html.replaceAll(REGEXP_SPACE + "<dt>" + REGEXP_SPACE, "<dt>");
		html = html.replaceAll(REGEXP_SPACE + "</dt>" + REGEXP_SPACE, "</dt>");
		html = html.replaceAll(REGEXP_SPACE + "<dd>" + REGEXP_SPACE, "<dd>");
		html = html.replaceAll(REGEXP_SPACE + "</dd>" + REGEXP_SPACE, "</dd>");

		String title = StringUtils.substringBetween(html, "<dt>Название документа:</dt><dd>", "<br/></dd>");
		final String number = StringUtils.substringBetween(html, "<dt>Номер документа:</dt><dd>", "</dd>");

		html = StringUtils.replace(html, "&nbsp;", "&#xA0;");
		html = StringUtils.replace(html, "\r", "\n");

		// System.out.println(html);

		html = StringUtils.substringBetween(html, "<a name=\"beginoftext\"></a>", "<div class=\"social_view\">");
		String wiki = html;

		// remove header
		wiki = StringUtils.substringAfter(wiki, "<P ID=\"P0005\" CLASS=\"formattext topleveltext\" ALIGN=\"justify\">");

		// mix cleanup
		wiki = wiki.replaceAll("<a href=\"/document//1\">", "");
		wiki = wiki.replaceAll("<a href=\"/document//1\" CONTEXT=\"[0-9A-Z]*\">", "");
		wiki = wiki.replaceAll("<a name=\"[0-9A-Z]*\" id=\"[0-9A-Z]*\" style=\"color:white\">якорь</a>", "");
		wiki = wiki.replaceAll("</[aA]>", "");
		if (wiki.contains("</TD></TR></TABLE>") && !wiki.contains("<TABLE")) {
			wiki = wiki.replaceAll("</TD></TR></TABLE>", "");
		}
		wiki = wiki.replaceAll("<span class='date'>([0-9а-я\\s]*)</span>", "$1");

		// well-known patterns
		wiki = wiki.replaceAll("<P ID=\"P[0-9A-F]*\" CLASS=\"[a-z ]*\"( ALIGN=\"[a-z]*\")*>" + "(\\s|\u00a0)*Президент"
				+ REGEXP_SPACE_OR_BR + "Российской" + REGEXP_SPACE_OR_BR + "Федерации" + REGEXP_SPACE_OR_BR + "Б."
				+ "(\\s|\u00a0)*" + "Ельцин" + "(<BR/>)*</P>",
				"\n\n{{Подпись|Президент Российской Федерации|Б. Ельцин}}\n\n");

		wiki = wiki.replaceAll("<P ID=\"P[0-9A-F]*\" CLASS=\"[a-z ]*\"( ALIGN=\"[a-z]*\")*>" + REGEXP_SPACE_OR_BR
				+ "Президент" + REGEXP_SPACE_OR_BR + "Российской" + REGEXP_SPACE_OR_BR + "Федерации"
				+ REGEXP_SPACE_OR_BR + "В.Путин" + "(<BR/>)*</P>",
				"\n\n{{Подпись|Президент Российской Федерации|В. Путин}}\n\n");

		wiki = wiki.replaceAll("<P ID=\"P[0-9A-F]*\" CLASS=\"[a-z ]*\"( ALIGN=\"[a-z]*\")*>" + REGEXP_SPACE_OR_BR
				+ "Президент" + REGEXP_SPACE_OR_BR + "Российской" + REGEXP_SPACE_OR_BR + "Федерации"
				+ REGEXP_SPACE_OR_BR + "Д.Медведев" + REGEXP_SPACE_OR_BR + "</P>",
				"\n\n{{Подпись|Президент Российской Федерации|Д. Медведев}}\n\n");

		String dateLetters, dateNormal;
		{
			Pattern pattern = Pattern.compile("<P ID=\"P[0-9A-F]*\" CLASS=\"[a-z ]*\"( ALIGN=\"[a-z]*\")*>"
					+ REGEXP_SPACE_OR_BR + "Москва, Кремль" + REGEXP_SPACE_OR_BR + "([0-9]* [а-я]* [0-9]*) (года?)"
					+ REGEXP_SPACE_OR_BR + "N ([0-9а-я]*)" + REGEXP_SPACE_OR_BR
					+ "(Электронный текст документа([^>]|<BR/>)*)?" + REGEXP_SPACE_OR_BR + "</P>");
			Matcher matcher = pattern.matcher(wiki);
			if (matcher.find()) {

				dateLetters = matcher.group(4);
				dateNormal = toDate(dateLetters);

				wiki = matcher.replaceFirst("\n\n{{right|Москва, Кремль}}\n{{right|$4 $5}}\n{{right|№ $7}}\n\n");
			} else {
				throw new Exception("No standart signature with date and number");
			}
		}

		wiki = wiki.replaceAll("<P ID=\"P[0-9A-F]*\" CLASS=\"[a-z ]*\"( ALIGN=\"[a-z]*\")*>" + REGEXP_SPACE_OR_BR
				+ "Москва, Кремль" + REGEXP_SPACE_OR_BR + "([0-9]* [а-я]* [0-9]* года?)" + REGEXP_SPACE_OR_BR
				+ "N ([0-9]*)" + REGEXP_SPACE_OR_BR + "</P>",
				"\n\n{{right|Москва, Кремль}}\n{{right|$4}}\n{{right|№&nbsp;$6}}\n\n");

		// Утверждено Указом Президента...
		wiki = wiki.replaceAll("<P ID=\"P[0-9A-F]*\" CLASS=\"formattext topleveltext\"( ALIGN=\"[a-z]*\")*>"
				+ REGEXP_SPACE_OR_BR + "(УТВЕРЖДЕН|УТВЕРЖДЕНО)" + REGEXP_SPACE_OR_BR + "Указом Президента"
				+ REGEXP_SPACE_OR_BR + "Российской Федерации" + REGEXP_SPACE_OR_BR + "(от [0-9]+ [а-я]+ [0-9]+ года?)"
				+ REGEXP_SPACE_OR_BR + "N ([0-9]*)" + REGEXP_SPACE_OR_BR + "</P>",
				"\n\n----\n\n{{right|$3<br/>Указом Президента<br/>Российской Федерации<br/>$7 №&nbsp;$9}}\n\n");

		// Приложение к указу Президента...
		wiki = wiki
				.replaceAll("<P ID=\"P[0-9A-F]*\" CLASS=\"formattext topleveltext\"( ALIGN=\"[a-z]*\")*>"
						+ REGEXP_SPACE_OR_BR + "Приложение" + REGEXP_SPACE_OR_BR + "к Указу Президента"
						+ REGEXP_SPACE_OR_BR + "Российской Федерации" + REGEXP_SPACE_OR_BR
						+ "(от [0-9]+ [а-я]+ [0-9]+ года?)" + REGEXP_SPACE_OR_BR + "N ([0-9]*)" + REGEXP_SPACE_OR_BR
						+ "</P>",
						"\n\n----\n\n{{right|Приложение<br/>к Указу Президента<br/>Российской Федерации<br/>$6 №&nbsp;$8}}\n\n");

		// Приложение N 1 к указу Президента...
		wiki = wiki
				.replaceAll("<P ID=\"P[0-9A-F]*\" CLASS=\"formattext topleveltext\"( ALIGN=\"[a-z]*\")*>"
						+ REGEXP_SPACE_OR_BR + "Приложение N ([0-9]*)" + REGEXP_SPACE_OR_BR + "к Указу Президента"
						+ REGEXP_SPACE_OR_BR + "Российской Федерации" + REGEXP_SPACE_OR_BR
						+ "(от [0-9]+ [а-я]+ [0-9]+ года?)" + REGEXP_SPACE_OR_BR + "N ([0-9]*)" + REGEXP_SPACE_OR_BR
						+ "</P>",
						"\n\n----\n\n{{right|Приложение №&nbsp;$3<br/>к Указу Президента<br/>Российской Федерации<br/>$7 №&nbsp;$9}}\n\n");

		// other texts
		wiki = wiki.replaceAll(REGEXP_SPACE_OR_BR + "</P>", "</P>");
		wiki = wiki.replaceAll("<P([^>]*)>" + REGEXP_SPACE_OR_BR, "<P$1>");
		wiki = wiki.replaceAll("<P>" + REGEXP_SPACE_OR_BR, "<P$1>");
		wiki = wiki.replaceAll(">[\\s\u00a0]*<BR/>", "><BR/>");
		wiki = wiki.replaceAll("<BR/>[\\s\u00a0]*<", "<BR/><");
		wiki = wiki.replaceAll("<BR/>", "\n\n");

		wiki = wiki.replaceAll("<P [^>]*></P>", "\n\n");
		wiki = wiki.replaceAll("<B>", "'''");
		wiki = wiki.replaceAll("</B>", "'''");

		// internal links
		wiki = wiki.replaceAll("<div id=\"soderjanie-link\">.*</div>", "");
		wiki = wiki.replaceAll("<a name=\"[0-9A-Z]+\" id=\"P[0-9A-F]+\"></a>", "");

		// images
		wiki = new Images().process(httpManager.getClient(HttpManager.DEFAULT_CLIENT), wiki);

		wiki = wiki.replaceAll(
				"<P ID=\"P[0-9A-F]*\" CLASS=\"headertext topleveltext centertext\" ALIGN=\"center\">([^<]*)</P>",
				"\n\n<center>'''$1'''</center>\n\n");
		wiki = wiki.replaceAll("<P ID=\"P[0-9A-Z]*\" CLASS=\"formattext\" ALIGN=\"center\">'''([^<]*)'''"
				+ REGEXP_SPACE_OR_BR + "</P>", "\n\n<center>'''$1'''</center>\n\n");
		wiki = wiki.replaceAll(REGEXP_SPACE_OR_BR + "<h2 id=\"h_[0-9A-Z]*\">([^<]*)" + REGEXP_SPACE_OR_BR + "</h2>",
				"\n\n== $2 ==\n\n");

		wiki = wiki.replaceAll("<P ID=\"P[0-9A-Z]*\" CLASS=\"formattext topleveltext\" ALIGN=\"right\">([^<]*)</P>",
				"\n\n{{right|$1}}\n\n");

		wiki = wiki.replaceAll("<P [^>]*>", "\n\n");
		wiki = wiki.replaceAll("<P>", "\n\n");
		wiki = wiki.replaceAll("</P>", "\n\n");

		wiki = wiki.replaceAll("<TABLE [^>]*>", "\n\n{|\n\n");
		wiki = wiki.replaceAll("</TABLE>", "\n\n|}\n\n");
		wiki = wiki.replaceAll("<TR[^>]*>", "\n\n|-\n\n");
		wiki = wiki.replaceAll("</TR>", "\n\n");
		wiki = wiki.replaceAll("<TD[^>]*(colspan|COLSPAN)=\"([0-9]*)\"[^>]*>", "\n\n| colspan=\"$2\" |\n\n");
		wiki = wiki.replaceAll("<TD[^>]*(rowspan|ROWSPAN)=\"([0-9]*)\"[^>]*>", "\n\n| rowspan=\"$2\" |\n\n");
		wiki = wiki.replaceAll("<TD[^>]*>", "\n\n|\n\n");
		wiki = wiki.replaceAll("</TD>", "\n\n");

		wiki = wiki.replaceAll("\n\\{\\|\n(\n)+", "\n{|\n");
		wiki = wiki.replaceAll("\n(\n)+\\|\\}\n", "\n|}\n");
		wiki = wiki.replaceAll("\n\n\\|\\-", "\n|-");
		wiki = wiki.replaceAll("\n\\|\\-\n(\n)+", "\n|-\n");
		wiki = wiki.replaceAll("(\n)+\n\\|", "\n|");
		wiki = wiki.replaceAll("\n\\|(\n)+([^\\|\n])", "\n| $2");
		wiki = wiki.replaceAll("(\n)+\\{\\|\n\\|\\-\\n\\|", "\n{|\n|");
		wiki = wiki.replaceAll("(\\n\\{\\|\\n)(\\| &#xA0;\\n)+\\|\\-\\n", "$1");

		while (wiki != wiki.replaceAll("(\\| [^\n]+)\\n\\n([^\n]+)", "$1 $2")) {
			wiki = wiki.replaceAll("(\\| [^\n]+)\\n\\n([^\n]+)", "$1 $2");
		}

		wiki = wiki.replaceAll("\\n\\| \\-&quot;\\-\\n", "\n| <nowiki>-''-</nowiki>\n");
		wiki = wiki.replaceAll("\\n\\{\\|\\n\\| (N п\\/п)\\n\\| ([^\\n\\|]+)\\n\\| ([^\\n\\|]+)\\n\\|\\-",
				"\n{| class=\"wikitable\" |\n! $1\n! $2\n! $3\n|-");

		wiki = wiki.replaceAll("\n\u00A0+\n", "\n\n");
		wiki = wiki.replaceAll("&quot;", "\"");

		wiki = "\n" + "\n" + wiki + "\n";

		while (wiki.contains("\n\n\n") || wiki.contains("\n ") || wiki.contains(" \n") || wiki.contains("\n\n\u00a0")) {
			wiki = StringUtils.replace(wiki, "\n\n\n", "\n\n");
			wiki = StringUtils.replace(wiki, "\n ", "\n");
			wiki = StringUtils.replace(wiki, " \n", "\n");
			wiki = StringUtils.replace(wiki, "\n\n\u00a0", "\n\n");
		}
		wiki = StringUtils.trim(wiki);

		wiki = wiki.replaceAll("^" + //
				"(<center>''')?" + "УКАЗ" + "('''</center>)?" + REGEXP_SPACE_OR_BR //
				+ "(<center>''')?" + "ПРЕЗИДЕНТА РОССИЙСКОЙ ФЕДЕРАЦИИ" + "('''</center>)?" + REGEXP_SPACE_OR_BR //
				+ "(<center>''')?" + Pattern.quote(title) + "('''</center>)?" //
		, "");

		wiki = wiki.replaceAll("(\n<center>'''.*)\n\n([^<]*'''</center>\n)", "$1 $2");

		wiki = wiki.replaceAll("^([0-9]*)\\. ", "'''$1.''' ");
		wiki = wiki.replaceAll("\n([0-9]*)\\. ", "\n'''$1.''' ");
		wiki = wiki.replaceAll("N[ \\t]*([0-9]+),[ \\t]*ст\\.[ \\t]*([0-9]+)", "№&nbsp;$1, ст.&nbsp;$2");
		wiki = wiki.replaceAll("(\nПримечания\\:) 1\\. ([^\n]*\n\n'''2\\.''')", "$1\n\n'''1.''' $2");
		wiki = wikify(wiki);

		wiki = StringUtils.substringBefore(wiki, "Электронный текст документа\n\n" + "подготовлен ЗАО «Кодекс»");

		wiki = addHyperlinks(wiki);

		title = wikify(title);
		title = addHyperlinks(title);

		wiki = "{{Указ Президента РФ\n" //
				+ "| НОМЕР          = " + number + "\n" //
				+ "| ДАТА           = " + dateNormal + "\n" //
				+ "| НАЗВАНИЕ       = " + title + "\n" //
				+ "| ДАТАПУБЛИКАЦИИ = \n" //
				+ "| ИСТОЧНИК       = [" + url + " Электронный фонд нормативных документов «Кодекс»]\n" //
				+ "| ДРУГОЕ         = \n" //
				+ "| УТРАТИЛ СИЛУ   = \n" //
				+ "| ПРЕДЫДУЩИЙ     = \n" //
				+ "| СЛЕДУЮЩИЙ      = \n" //
				+ "| КАЧЕСТВО       = \n" //
				+ "}}\n"

				+ "__TOC__\n\n" + "" + StringUtils.trim(wiki) + "\n" + "\n" + "{{RusGov}}" + "\n";

		System.out.println();
		final String pageTitle = "Указ Президента РФ от " + dateNormal + " № " + number;
		System.out.println("Page title: " + pageTitle);
		System.out.println();

		System.out.println(wiki);

		if (save)
			save(url, pageTitle, wiki);
	}

	private void save(String url, final String pageTitle, String wiki) throws Exception, URISyntaxException {
		mediaWikiBot.writeContent(pageTitle, null, wiki, null, "Import from " + url, false, false);
	}

	private String toDate(String dateLetters) throws ParseException {
		dateLetters = dateLetters.replace(" января ", ".01.");
		dateLetters = dateLetters.replace(" февраля ", ".02.");
		dateLetters = dateLetters.replace(" марта ", ".03.");
		dateLetters = dateLetters.replace(" апреля ", ".04.");
		dateLetters = dateLetters.replace(" мая ", ".05.");
		dateLetters = dateLetters.replace(" июня ", ".06.");
		dateLetters = dateLetters.replace(" июля ", ".07.");
		dateLetters = dateLetters.replace(" августа ", ".08.");
		dateLetters = dateLetters.replace(" сентября ", ".09.");
		dateLetters = dateLetters.replace(" октября ", ".10.");
		dateLetters = dateLetters.replace(" ноября ", ".11.");
		dateLetters = dateLetters.replace(" декабря ", ".12.");

		if (dateLetters.indexOf(".") == 1)
			dateLetters = "0" + dateLetters;

		return dateLetters;
	}

	private String wikify(String wiki) {
		wiki = wiki.replaceAll("([',\\.\\s\\r\\n\\(])" + "\"([^\"]+)\"" + "([',\\.\\:\\;\\s\\r\\n\\)]|$)", "$1«$2»$3");
		wiki = wiki.replaceAll("( )-([ \\n])", "$1—$2");

		wiki = wiki.replaceAll("(больницы|года|поликлинники|приложении|управления|участка) N ([0-9])", "$1 №&nbsp;$2");
		wiki = wiki.replaceAll("N п/п", "№ п/п");

		return wiki;
	}
}
