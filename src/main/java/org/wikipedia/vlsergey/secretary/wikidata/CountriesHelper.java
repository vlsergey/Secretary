package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByCategoryMembers.CmType;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikibaseEntityIdValue;

@Component
public class CountriesHelper extends AbstractHelper {

	public static Map<EntityId, EntityId> SPORTS_COUNTRIES = new HashMap<>();

	public static List<DataValue> VALUES_RUSSIA = Arrays.asList(new WikibaseEntityIdValue(Places.Россия));
	public static List<DataValue> VALUES_RUSSIAN_EMPIRE = Arrays.asList(new WikibaseEntityIdValue(
			Places.Российская_империя));
	public static List<DataValue> VALUES_RUSSIAN_EMPIRE_USSR = Arrays.asList(new WikibaseEntityIdValue(
			Places.Российская_империя), new WikibaseEntityIdValue(Places.СССР));
	public static List<DataValue> VALUES_USSR = Arrays.asList(new WikibaseEntityIdValue(Places.СССР));
	public static List<DataValue> VALUES_USSR_RUSSIA = Arrays.asList(new WikibaseEntityIdValue(Places.СССР),
			new WikibaseEntityIdValue(Places.Россия));

	static {
		SPORTS_COUNTRIES.put(Places.Англия, Places.Великобритания);
		SPORTS_COUNTRIES.put(Places.Северная_Ирландия, Places.Великобритания);
		SPORTS_COUNTRIES.put(Places.Уэльс, Places.Великобритания);
		SPORTS_COUNTRIES.put(Places.Шотландия, Places.Великобритания);
		SPORTS_COUNTRIES.put(Places.Китайский_Тайбэй, Places.Китайская_Республика);
	}

	private Map<String, String> DICTIONARY = new HashMap<>();

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	{
		addToDictionary("{{ger|1804}}", "Германия");
		addToDictionary("{{ger|1836}}", "Германия");
		addToDictionary("{{ger|1902}}", "Германия");
		addToDictionary("{{ger|1905}}", "Германия");

		addToDictionary("BUL", "Болгария");
		addToDictionary("CHI", "Чили");
		addToDictionary("CRO", "Хорватия");
		addToDictionary("gb", "Великобритания");
		addToDictionary("ger", "Германия");
		addToDictionary("gre", "Греция");
		addToDictionary("ind", "Индия");
		addToDictionary("irl", "Ирландия");
		addToDictionary("lat", "Латвия");
		addToDictionary("mda", "Молдавия");
		addToDictionary("mdb", "Молдавия");
		addToDictionary("ROM", "Румыния");
		addToDictionary("rsa", "Южно-Африканская Республика");
		addToDictionary("ru", "Россия");
		addToDictionary("sui", "Швейцария");
		addToDictionary("sun", "СССР");
		addToDictionary("uk", "Великобритания");
		addToDictionary("us", "США");
		addToDictionary("urs", "СССР");
		addToDictionary("URSS", "СССР");

		addToDictionary("Российская федерация", "Россия");

		addToDictionaryWithFlag("Австралии", "Австралия");
		addToDictionaryWithFlag("Австрии", "Австрия");
		addToDictionaryWithFlag("Австро-Венгрии", "Австро-Венгрия");
		addToDictionaryWithFlag("Азербайджана", "Азербайджан");
		addToDictionaryWithFlag("Албании", "Албания");
		addToDictionaryWithFlag("Алжира", "Алжир");
		addToDictionaryWithFlag("Американских Виргинских островов", "Американские Виргинские острова");
		addToDictionaryWithFlag("Англии", "Англия");
		addToDictionaryWithFlag("Анголы", "Ангола");
		addToDictionaryWithFlag("Андорры", "Андорра");
		addToDictionaryWithFlag("Антигуа и Барбуда", "Антигуа и Барбуда");
		addToDictionaryWithFlag("Аргентины", "Аргентина");
		/**/addToDictionaryWithFlag("Аргентины (1812-1985)", "Аргентина");
		addToDictionaryWithFlag("Армении", "Армения");
		addToDictionaryWithFlag("Афганистана", "Афганистан");
		addToDictionaryWithFlag("Багам", "Багамы");
		addToDictionaryWithFlag("Багамских островов", "Багамы");
		addToDictionaryWithFlag("Беларуси", "Беларусь");
		/**/addToDictionaryWithFlag("Белоруссии", "Белоруссия");
		/**/addToDictionaryWithFlag("Белоруссии", "Беларусь");
		/**/addToDictionaryWithFlag("Белоруссии (1995-2012)", "Белоруссия");
		addToDictionaryWithFlag("Бельгии", "Бельгия");
		addToDictionaryWithFlag("Бенина", "Бенин");
		addToDictionaryWithFlag("Болгарии", "Болгария");
		addToDictionaryWithFlag("Боливии", "Боливия");
		addToDictionaryWithFlag("Боснии и Герцеговины", "Босния и Герцеговина");
		/**/addToDictionaryWithFlag("Боснии", "Босния и Герцеговина");
		addToDictionaryWithFlag("Бразилии", "Бразилия");
		/**/addToDictionaryWithFlag("Бразилии (1968-1992)", "Бразилия");
		addToDictionaryWithFlag("БССР", "БССР");
		addToDictionaryWithFlag("Буркина-Фасо", "Буркина-Фасо");
		addToDictionaryWithFlag("Веймарской республики", "Веймарская республика");
		addToDictionaryWithFlag("Великобритании", "Великобритания");
		addToDictionaryWithFlag("Венгрии", "Венгрия");
		/**/addToDictionaryWithFlag("Венгрии (1920-1940)", "Венгрия");
		addToDictionaryWithFlag("Венесуэлы", "Венесуэла");
		addToDictionaryWithFlag("Вьетнама", "Вьетнам");
		addToDictionaryWithFlag("Габона", "Габон");
		addToDictionaryWithFlag("Гайаны", "Гайана");
		addToDictionaryWithFlag("Гамбии", "Гамбия");
		addToDictionaryWithFlag("Ганы", "Гана");
		addToDictionaryWithFlag("Гвинеи", "Гвинея");
		addToDictionaryWithFlag("Германии", "Германия");
		addToDictionaryWithFlag("Германской империи", "Германская империя");
		addToDictionaryWithFlag("ГДР", "ГДР");
		addToDictionaryWithFlag("Гибралтара", "Гибралтар");
		addToDictionaryWithFlag("Голландской Ост-Индии", "Голландская Ост-Индия");
		addToDictionaryWithFlag("Гондураса", "Гондурас");
		addToDictionaryWithFlag("Гонконга", "Гонконг");
		addToDictionaryWithFlag("Греции", "Греция");
		addToDictionaryWithFlag("Грузии", "Грузия");
		addToDictionaryWithFlag("Дании", "Дания");
		addToDictionaryWithFlag("Доминиканской Республики", "Доминиканская Республика");
		addToDictionaryWithFlag("ДР Конго", "ДР Конго");
		addToDictionaryWithFlag("Египта", "Египет");
		/**/addToDictionaryWithFlag("Египта (1882-1922)", "Египет");
		addToDictionaryWithFlag("Заира", "Заир");
		addToDictionaryWithFlag("Замбии", "Замбия");
		addToDictionaryWithFlag("Зимбабве", "Зимбабве");
		addToDictionaryWithFlag("Израиля", "Израиль");
		addToDictionaryWithFlag("Индии", "Индия");
		addToDictionaryWithFlag("Индонезии", "Индонезия");
		addToDictionaryWithFlag("Иордании", "Иордания");
		addToDictionaryWithFlag("Ирака", "Ирак");
		addToDictionaryWithFlag("Ирана", "Иран");
		/**/addToDictionaryWithFlag("Ирана (1925-1964)", "Иран");
		addToDictionaryWithFlag("Ирландии", "Ирландия");
		addToDictionaryWithFlag("Исландии", "Исландия");
		addToDictionaryWithFlag("Испании", "Испания");
		/**/addToDictionaryWithFlag("Испании (1945-1977)", "Испания");
		addToDictionaryWithFlag("Италии", "Италия");
		/**/addToDictionaryWithFlag("Италии (1861-1946)", "Италия");
		addToDictionaryWithFlag("Кабо-Верде", "Кабо-Верде");
		addToDictionaryWithFlag("Казахстана", "Казахстан");
		addToDictionaryWithFlag("Камеруна", "Камерун");
		addToDictionaryWithFlag("Канады", "Канада");
		/**/addToDictionaryWithFlag("Канады (1868-1921)", "Канада");
		addToDictionaryWithFlag("Катара", "Катар");
		addToDictionaryWithFlag("Кении", "Кения");
		addToDictionaryWithFlag("Кипра", "Кипр");
		/**/addToDictionaryWithFlag("Кипра", "Республика Кипр");
		addToDictionaryWithFlag("Киргизии", "Киргизия");
		addToDictionaryWithFlag("Китая", "Китай");
		addToDictionaryWithFlag("Китая", "КНР");
		addToDictionaryWithFlag("КНДР", "КНДР");
		addToDictionaryWithFlag("Колумбии", "Колумбия");
		addToDictionaryWithFlag("Королевства Югославия", "Королевство Югославия");
		addToDictionaryWithFlag("Коста-Рики", "Коста-Рика");
		addToDictionaryWithFlag("Кот-д'Ивуара", "Кот-д'Ивуар");
		addToDictionaryWithFlag("Кот-д’Ивуара", "Кот-д’Ивуар");
		addToDictionaryWithFlag("Кубы", "Куба");
		addToDictionaryWithFlag("Кувейта", "Кувейт");
		addToDictionaryWithFlag("Латвии", "Латвия");
		addToDictionaryWithFlag("Либерии", "Либерия");
		addToDictionaryWithFlag("Литвы", "Литва");
		/**/addToDictionary("{{Флаг|Литва}} [[Литовская Республика]]", "Литовская Республика");
		addToDictionaryWithFlag("Лихтенштейна", "Лихтенштейн");
		addToDictionaryWithFlag("Люксембурга", "Люксембург");
		addToDictionaryWithFlag("Македонии", "Республика Македония");
		addToDictionaryWithFlag("Малайзии", "Малайзия");
		addToDictionaryWithFlag("Мали", "Мали");
		addToDictionaryWithFlag("Мальты", "Мальта");
		addToDictionaryWithFlag("Марокко", "Марокко");
		addToDictionaryWithFlag("Мексики", "Мексика");
		addToDictionaryWithFlag("Молдавии", "Молдавия");
		addToDictionaryWithFlag("Молдовы", "Молдова");
		addToDictionaryWithFlag("Монголии", "Монголия");
		addToDictionaryWithFlag("Намибии", "Намибия");
		addToDictionaryWithFlag("Непала", "Непал");
		addToDictionaryWithFlag("Нигера", "Нигер");
		addToDictionaryWithFlag("Нигерии", "Нигерия");
		addToDictionaryWithFlag("Нидерландов", "Нидерланды");
		addToDictionaryWithFlag("Никарагуа", "Никарагуа");
		addToDictionaryWithFlag("Новой Зеландии", "Новая Зеландия");
		addToDictionaryWithFlag("Норвегии", "Норвегия");
		addToDictionaryWithFlag("ОАЭ", "ОАЭ");
		addToDictionaryWithFlag("Омана", "Оман");
		addToDictionaryWithFlag("Османской империи", "Османская империя");
		addToDictionaryWithFlag("Пакистана", "Пакистан");
		addToDictionaryWithFlag("Панамы", "Панама");
		addToDictionaryWithFlag("Парагвая", "Парагвай");
		/**/addToDictionaryWithFlag("Парагвая (1842-1954)", "Парагвай");
		addToDictionaryWithFlag("Перу", "Перу");
		addToDictionaryWithFlag("Польши", "Польша");
		addToDictionaryWithFlag("Португалии", "Португалия");
		/**/addToDictionary("{{флаг|Пруссия}} [[Пруссия (королевство)|Королевство Пруссия]]", "Пруссия (королевство)");
		addToDictionaryWithFlag("Пуэрто-Рико", "Пуэрто-Рико");
		addToDictionaryWithFlag("Республики Корея", "Республика Корея");
		addToDictionaryWithFlag("Российской империи", "Российская империя");
		/**/addToDictionaryWithFlag("России", "Российская империя");
		addToDictionaryWithFlag("России", "Россия");
		/**/addToDictionaryWithFlag("России", "Российская Федерация");
		/**/addToDictionaryWithFlag("России (1991-1993)", "России");
		/**/addToDictionaryWithFlag("Российской Федерации", "Российская Федерация");
		/**/addToDictionaryWithFlag("РФ", "РФ");
		/**/addToDictionary("{{Флаг Россия|20px}} [[Россия]]", "Россия");
		addToDictionaryWithFlag("Румынии", "Румыния");
		/**/addToDictionaryWithFlag("Румынии (1965-1989)", "Румыния");
		addToDictionaryWithFlag("Сальвадора", "Сальвадор");
		addToDictionaryWithFlag("Самоа", "Самоа");
		addToDictionaryWithFlag("Саудовской Аравии", "Саудовская Аравия");
		addToDictionaryWithFlag("Северной Ирландии", "Северная Ирландия");
		addToDictionaryWithFlag("Сенегала", "Сенегал");
		addToDictionaryWithFlag("Сент-Китса и Невиса", "Сент-Китс и Невис");
		addToDictionaryWithFlag("Сербии", "Сербия");
		addToDictionaryWithFlag("Сербии и Черногории", "Сербия и Черногория");
		addToDictionaryWithFlag("Сирии", "Сирия");
		addToDictionaryWithFlag("Словакии", "Словакия");
		addToDictionaryWithFlag("Словении", "Словения");
		addToDictionaryWithFlag("СРЮ", "Сербия и Черногория");
		addToDictionaryWithFlag("СССР", "СССР");
		/**/addToDictionaryWithFlag("СССР (1923-1955)", "СССР");
		/**/addToDictionary("{{Флаг|СССР||20px}} [[Союз Советских Социалистических Республик|СССР]]", "СССР");
		addToDictionaryWithFlag("Судана", "Судан");
		addToDictionaryWithFlag("СФРЮ", "СФРЮ");
		addToDictionaryWithFlag("США", "США");
		/**/addToDictionary("{{Флаг США}} [[Соединённые Штаты Америки|США]]", "Соединённые Штаты Америки");
		/**/addToDictionary("{{Флаг США|20px}} [[Соединённые Штаты Америки|США]]", "Соединённые Штаты Америки");
		/**/addToDictionary("{{Флаг США|22px}} [[Соединённые Штаты Америки|США]]", "Соединённые Штаты Америки");
		addToDictionaryWithFlag("Таиланда", "Таиланд");
		addToDictionaryWithFlag("Того", "Того");
		addToDictionaryWithFlag("Тонга", "Тонга");
		addToDictionaryWithFlag("Третьего рейха", "Третий рейх");
		addToDictionaryWithFlag("Тринидада и Тобаго", "Тринидад и Тобаго");
		addToDictionaryWithFlag("Туниса", "Тунис");
		addToDictionaryWithFlag("Туркмении", "Туркменистан");
		addToDictionaryWithFlag("Туркмении", "Туркмения");
		addToDictionaryWithFlag("Турции", "Турция");
		addToDictionaryWithFlag("Уганды", "Уганда");
		addToDictionaryWithFlag("Узбекистана", "Узбекистан");
		addToDictionaryWithFlag("Украины", "Украина");
		addToDictionaryWithFlag("Уругвая", "Уругвай");
		addToDictionaryWithFlag("Уэльса", "Уэльс");
		addToDictionaryWithFlag("Фарерских островов", "Фарерские острова");
		addToDictionaryWithFlag("Филиппин", "Филиппины");
		addToDictionaryWithFlag("Финляндии", "Финляндия");
		addToDictionaryWithFlag("Франции", "Франция");
		addToDictionaryWithFlag("ФРГ", "ФРГ");
		addToDictionaryWithFlag("Хорватии", "Хорватия");
		/**/addToDictionaryWithFlag("Хорватии (1941-1945)", "Хорватия");
		addToDictionaryWithFlag("Черногории", "Черногория");
		addToDictionaryWithFlag("Чехии", "Чехия");
		addToDictionaryWithFlag("Чехословакии", "Чехословакия");
		addToDictionaryWithFlag("Чили", "Чили");
		addToDictionaryWithFlag("Швейцарии", "Швейцария");
		addToDictionaryWithFlag("Швеции", "Швеция");
		addToDictionaryWithFlag("Шотландии", "Шотландия");
		addToDictionaryWithFlag("Эквадора", "Эквадор");
		addToDictionaryWithFlag("Эстонии", "Эстония");
		addToDictionaryWithFlag("ЮАР", "ЮАР");
		addToDictionaryWithFlag("Югославии", "Югославия");
		addToDictionaryWithFlag("Южной Кореи", "Южная Корея");
		addToDictionaryWithFlag("Ямайки", "Ямайка");
		addToDictionaryWithFlag("Японии", "Япония");

		addToDictionary("{{Флаг Франции}}", "Франция");
		addToDictionary("{{Флаг Швеции}}", "Швеция");

	}

	private void addToDictionary(String key, String value) {
		DICTIONARY.put(key.toLowerCase(), value);
		DICTIONARY.put("{{" + key.toLowerCase() + "}}", value);
	}

	void addToDictionaryWithFlag(String flagOf, String country) {

		for (String prefix : new String[] { "", "Флаг ", "Флаг|" }) {
			for (String flagSize : new String[] { "", "|15px", "|20px", "|22px", "|35px" }) {
				for (boolean withSpace : new boolean[] { true, false }) {
					for (boolean withBrackets : new boolean[] { true, false }) {
						{
							StringBuilder stringBuilder = new StringBuilder();
							stringBuilder.append("{{");
							stringBuilder.append(prefix);
							stringBuilder.append(flagOf);
							stringBuilder.append(flagSize);
							stringBuilder.append("}}");
							if (withSpace) {
								stringBuilder.append(" ");
							}
							if (withBrackets) {
								stringBuilder.append("[[");
							}
							stringBuilder.append(country);
							if (withBrackets) {
								stringBuilder.append("]]");
							}
							addToDictionary(stringBuilder.toString(), country);
						}
						for (boolean withEmptyParameter : new boolean[] { true, false }) {
							StringBuilder stringBuilder = new StringBuilder();
							stringBuilder.append("{{");
							stringBuilder.append(prefix);
							stringBuilder.append(country);
							if (StringUtils.isEmpty(flagSize)) {
								if (withEmptyParameter) {
									stringBuilder.append("|");
								}
							} else {
								stringBuilder.append("|");
								stringBuilder.append(flagSize);
							}
							stringBuilder.append("}}");
							if (withSpace) {
								stringBuilder.append(" ");
							}
							if (withBrackets) {
								stringBuilder.append("[[");
							}
							stringBuilder.append(country);
							if (withBrackets) {
								stringBuilder.append("]]");
							}
							addToDictionary(stringBuilder.toString(), country);
						}

					}
				}
			}
		}
	}

	@Override
	public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipediaSnaks,
			Collection<ValueWithQualifiers> wikidataSnaks) {

		if (wikipediaSnaks.isEmpty()) {
			return ReconsiliationAction.remove_from_wikipedia_as_empty;
		}
		if (wikidataSnaks.isEmpty()) {
			return ReconsiliationAction.append;
		}

		List<DataValue> wikipedia = wikipediaSnaks.stream()
				.map(x -> x.getValue().getSnakType() == SnakType.value ? x.getValue().getDataValue() : null)
				.collect(Collectors.toList());
		List<DataValue> wikidata = wikidataSnaks.stream()
				.map(x -> x.getValue().getSnakType() == SnakType.value ? x.getValue().getDataValue() : null)
				.collect(Collectors.toList());

		if (wikipedia.equals(VALUES_RUSSIAN_EMPIRE) && wikidata.equals(VALUES_RUSSIA)) {
			return ReconsiliationAction.replace;
		} else if (wikipedia.equals(VALUES_RUSSIAN_EMPIRE_USSR) && wikidata.equals(VALUES_RUSSIA)) {
			return ReconsiliationAction.replace;
		} else if (wikipedia.equals(VALUES_USSR_RUSSIA) && wikidata.equals(VALUES_RUSSIA)) {
			return ReconsiliationAction.replace;
		} else if (wikipedia.equals(VALUES_USSR) && wikidata.equals(VALUES_RUSSIA)) {
			return ReconsiliationAction.replace;
		}

		if (wikidata.containsAll(wikipedia)) {
			return ReconsiliationAction.remove_from_wikipedia_as_not_empty;
		}
		return ReconsiliationAction.report_difference;
	}

	@PostConstruct
	public void init() {
		for (Revision revision : ruWikipediaCache.queryByCaterogyMembers("Category:Шаблоны:Флаги по коду",
				new Namespace[] { Namespace.TEMPLATE }, CmType.page)) {
			final String content = revision.getContent();
			String country = null;
			if (content.contains("{{Флагификация|")) {
				country = StringUtils.substringBetween(content, "{{Флагификация|", "}}");
				if (country.contains("|")) {
					country = StringUtils.substringBefore(country, "|");
				}
				country = country.trim();
			} else if (content.contains("{{флагификация|")) {
				country = StringUtils.substringBetween(content, "{{флагификация|", "}}");
				if (country.contains("|")) {
					country = StringUtils.substringBefore(country, "|");
				}
				country = country.trim();
			}
			if (country != null) {
				addToDictionary(revision.getPage().getTitle(), country);
				addToDictionary(revision.getPage().getTitle().substring("Шаблон:".length()), country);

				DICTIONARY.put("{{" + country.toLowerCase() + "}}", country);
				DICTIONARY.put("{{" + country.toLowerCase() + "|20px}}", country);
			}
		}
	}

	public List<String> normalize(String strValue) {
		strValue = StringUtils.replace(strValue, "</br>", ";");
		strValue = StringUtils.replace(strValue, "<br / >", ";");
		strValue = StringUtils.replace(strValue, "<b r/>", ";");
		strValue = StringUtils.replace(strValue, "<br>", ";");
		strValue = StringUtils.replace(strValue, "<BR>", ";");
		strValue = StringUtils.replace(strValue, "<br/>", ";");
		strValue = StringUtils.replace(strValue, "<br />", ";");
		strValue = StringUtils.replace(strValue, "=>", "→");

		strValue = strValue.replaceAll("\\[\\[Файл\\:[^\\]]+\\]\\]", "");
		strValue = strValue.replaceAll("\\[\\[File\\:[^\\]]+\\]\\]", "");

		strValue = strValue.trim();

		if (strValue.startsWith("Подданство ")) {
			strValue = strValue.substring("Подданство ".length());
		}

		List<String> result = new ArrayList<>();
		for (String token : StringUtils.split(strValue, ",;→/")) {
			token = StringUtils.trim(token);
			if (StringUtils.isBlank(token)) {
				continue;
			}
			if (token.startsWith("{{Флагификация|") && token.endsWith("}}")
					&& token.indexOf("}}") == token.length() - "}}".length()) {
				String country = token.substring("{{Флагификация|".length(), token.length() - "}}".length());
				if (country.contains("|")) {
					country = StringUtils.substringBefore(country, "|");
				}
				token = country.trim();
			}
			if (token.startsWith("{{флагификация|") && token.endsWith("}}")
					&& token.indexOf("}}") == token.length() - "}}".length()) {
				String country = token.substring("{{флагификация|".length(), token.length() - "}}".length());
				if (country.contains("|")) {
					country = StringUtils.substringBefore(country, "|");
				}
				token = country.trim();
			}
			if (token.startsWith("[[") && token.endsWith("]]") && token.indexOf("]]") == token.length() - 2) {
				String country = token.substring("[[".length(), token.length() - "]]".length());
				if (country.contains("|")) {
					country = StringUtils.substringBefore(country, "|");
				}
				token = country.trim();
			}
			if (token.matches("^\\{\\{([А-Яа-я]+)\\}\\}\\s+\\[\\[\\1\\]\\]$")) {
				token = StringUtils.substringBetween(token, "{{", "}}");
			}
			if (token.matches("^\\{\\{([А-Яа-я]+)\\}\\}$")) {
				token = StringUtils.substringBetween(token, "{{", "}}");
			}
			if (token.matches("^\\{\\{Флаг\\|([А-Яа-я\\s]+)\\}\\}\\s+\\[\\[\\1\\]\\]$")) {
				token = StringUtils.substringBetween(token, "{{Флаг|", "}}");
			}
			if (token.matches("^\\{\\{Флаг\\|([А-Яа-я\\s]+)\\}\\}\\s+$")) {
				token = StringUtils.substringBetween(token, "{{Флаг|", "}}");
			}
			if (DICTIONARY.containsKey(token.toLowerCase())) {
				token = DICTIONARY.get(token.toLowerCase());
			}
			result.add(token);
		}
		return result;
	}

	public synchronized List<ValueWithQualifiers> parse(final EntityByLinkResolver entityByLinkResolver,
			EntityId property, String strValue) {
		List<ValueWithQualifiers> result = new ArrayList<>();

		strValue = StringUtils.replace(strValue, "&nbsp;", " ");
		strValue = strValue.trim();

		if (UNKNOWN.contains(strValue.toLowerCase())) {
			return ValueWithQualifiers.fromSnak(Snak.newSnak(property, SnakType.somevalue));
		}

		if (StringUtils.equalsIgnoreCase("{{Российская империя}} {{USSR}}", strValue)) {
			return Arrays
					.asList(new ValueWithQualifiers(Snak.newSnak(property, Places.Российская_империя), Collections
							.emptyList()),
							new ValueWithQualifiers(Snak.newSnak(property, Places.СССР), Collections.emptyList()));
		}
		if (StringUtils.equalsIgnoreCase("{{USSR}} {{RUS}}", strValue)
				|| StringUtils.equalsIgnoreCase("{{Флагификация|СССР}} {{Флагификация|Россия}}", strValue)) {
			return Arrays.asList(new ValueWithQualifiers(Snak.newSnak(property, Places.СССР), Collections.emptyList()),
					new ValueWithQualifiers(Snak.newSnak(property, Places.Россия), Collections.emptyList()));
		}
		if (StringUtils.equalsIgnoreCase("{{USSR}} {{UKR}}", strValue)) {
			return Arrays.asList(new ValueWithQualifiers(Snak.newSnak(property, Places.СССР), Collections.emptyList()),
					new ValueWithQualifiers(Snak.newSnak(property, Places.Украина), Collections.emptyList()));
		}

		List<String> countryNames = normalize(strValue);
		for (String countryName : countryNames) {
			Entity entity = entityByLinkResolver.apply(countryName);
			if (entity == null) {
				throw new NoWikidataElementException(countryName);
			}

			result.add(new ValueWithQualifiers(Snak.newSnak(property, entity.getId()), Collections.emptyList()));
		}
		return result;
	}
}
