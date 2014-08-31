package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikibaseEntityIdValue;

@Component
public class CountriesHelper {

	private static final EntityId COUNTRY_RUSSIA = EntityId.item(159l);
	private static final EntityId COUNTRY_RUSSIAN_EMPIRE = EntityId.item(34266l);
	private static final EntityId COUNTRY_USSR = EntityId.item(15180l);

	public static List<DataValue> VALUES_RUSSIA = Arrays.asList(new WikibaseEntityIdValue(COUNTRY_RUSSIA));
	public static List<DataValue> VALUES_RUSSIAN_EMPIRE = Arrays.asList(new WikibaseEntityIdValue(
			COUNTRY_RUSSIAN_EMPIRE));
	public static List<DataValue> VALUES_RUSSIAN_EMPIRE_USSR = Arrays.asList(new WikibaseEntityIdValue(
			COUNTRY_RUSSIAN_EMPIRE), new WikibaseEntityIdValue(COUNTRY_USSR));
	public static List<DataValue> VALUES_USSR = Arrays.asList(new WikibaseEntityIdValue(COUNTRY_USSR));
	public static List<DataValue> VALUES_USSR_RUSSIA = Arrays.asList(new WikibaseEntityIdValue(COUNTRY_USSR),
			new WikibaseEntityIdValue(COUNTRY_RUSSIA));

	private Map<String, String> DICTIONARY = new HashMap<>();

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	{
		DICTIONARY.put("{{ger|1804}}", "Германия");
		DICTIONARY.put("{{ger|1836}}", "Германия");
		DICTIONARY.put("{{ger|1902}}", "Германия");
		DICTIONARY.put("{{ger|1905}}", "Германия");

		DICTIONARY.put("gb", "Великобритания");
		DICTIONARY.put("ger", "Германия");
		DICTIONARY.put("ind", "Индия");
		DICTIONARY.put("irl", "Ирландия");
		DICTIONARY.put("lat", "Латвия");
		DICTIONARY.put("mdb", "Молдавия");
		DICTIONARY.put("ru", "Россия");
		DICTIONARY.put("sui", "Швейцария");
		DICTIONARY.put("sun", "СССР");
		DICTIONARY.put("uk", "Великобритания");
		DICTIONARY.put("urs", "СССР");
		DICTIONARY.put("URSS".toLowerCase(), "СССР");

		DICTIONARY.put("Российская федерация".toLowerCase(), "Россия");

		DICTIONARY.put("{{Флаг Азербайджана}} [[Азербайджан]]".toLowerCase(), "Азербайджан");
		DICTIONARY.put("{{Флаг Бельгии}} [[Бельгия]]".toLowerCase(), "Бельгия");
		DICTIONARY.put("{{Флаг Веймарской республики}} [[Веймарская республика]]".toLowerCase(),
				"Веймарская республика");
		DICTIONARY.put("{{Флаг Казахстана}} [[Казахстан]]".toLowerCase(), "Казахстан");
		DICTIONARY.put("{{Флаг|Литва}} [[Литовская Республика]]".toLowerCase(), "Литовская Республика");
		DICTIONARY.put("{{Флаг Нидерландов|22px}} [[Нидерланды]]".toLowerCase(), "Нидерланды");
		DICTIONARY.put("{{Флаг РФ}} [[РФ]]".toLowerCase(), "Россия");
		DICTIONARY.put("{{Флаг СССР}} [[СССР]]".toLowerCase(), "СССР");
		DICTIONARY.put("{{Флаг Третьего рейха}} [[Третий рейх]]".toLowerCase(), "Третий рейх");
		DICTIONARY.put("{{Флаг Финляндии|35px}} [[Финляндия]]".toLowerCase(), "Финляндия");
		DICTIONARY.put("{{Флаг ФРГ}} [[ФРГ]]".toLowerCase(), "ФРГ");
		DICTIONARY.put("{{Флаг Японии}} [[Япония]]".toLowerCase(), "Япония");

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
				DICTIONARY.put("{{" + revision.getPage().getTitle().toLowerCase() + "}}", country);
				DICTIONARY.put("{{" + revision.getPage().getTitle().toLowerCase().substring("шаблон:".length()) + "}}",
						country);
			}
		}
	}

	public List<String> normalize(String strValue) {
		strValue = StringUtils.replace(strValue, "&nbsp;", " ");
		strValue = StringUtils.replace(strValue, "</br>", ";");
		strValue = StringUtils.replace(strValue, "<b r/>", ";");
		strValue = StringUtils.replace(strValue, "<br>", ";");
		strValue = StringUtils.replace(strValue, "<br/>", ";");
		strValue = StringUtils.replace(strValue, "<br />", ";");

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
			if (DICTIONARY.containsKey(token.toLowerCase())) {
				token = DICTIONARY.get(token.toLowerCase());
			}
			result.add(token);
		}
		return result;
	}

}
