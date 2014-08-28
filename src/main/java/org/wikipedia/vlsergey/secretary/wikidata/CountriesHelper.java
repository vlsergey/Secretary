package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
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

@Component
public class CountriesHelper {

	private Map<String, String> DICTIONARY = new HashMap<>();
	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;
	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	{
		DICTIONARY.put("{{gb}}", "Великобритания");
		DICTIONARY.put("{{ger}}", "Германия");
		DICTIONARY.put("{{ger|1804}}", "Германия");
		DICTIONARY.put("{{ger|1836}}", "Германия");
		DICTIONARY.put("{{ger|1902}}", "Германия");
		DICTIONARY.put("{{ger|1905}}", "Германия");
		DICTIONARY.put("{{ind}}", "Индия");
		DICTIONARY.put("{{irl}}", "Ирландия");
		DICTIONARY.put("{{lat}}", "Латвия");
		DICTIONARY.put("{{sun}}", "СССР");
		DICTIONARY.put("{{uk}}", "Великобритания");
		DICTIONARY.put("{{urs}}", "СССР");

		DICTIONARY.put("{{aзербайджан}}", "Aзербайджан");
		DICTIONARY.put("{{великобритания}}", "Великобритания");
		DICTIONARY.put("{{россия}}", "Россия");
		DICTIONARY.put("{{россия}}", "Россия");
		DICTIONARY.put("{{сша}}", "США");
		DICTIONARY.put("{{франция}}", "Франция");

		DICTIONARY.put("Российская федерация", "Россия");
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
		strValue = StringUtils.replace(strValue, "<b r/>", ";");
		strValue = StringUtils.replace(strValue, "<br>", ";");
		strValue = StringUtils.replace(strValue, "<br/>", ";");
		strValue = StringUtils.replace(strValue, "<br />", ";");

		strValue.replace("\\[\\[Файл\\:[^\\]]\\]\\]", "");
		strValue.replace("\\[\\[File\\:[^\\]]\\]\\]", "");

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
			if (token.startsWith("{{Флагификация|") && token.endsWith("}}")) {
				String country = token.substring("{{Флагификация|".length(), token.length() - "}}".length());
				if (country.contains("|")) {
					country = StringUtils.substringBefore(country, "|");
				}
				token = country.trim();
			}
			if (token.startsWith("{{флагификация|") && token.endsWith("}}")) {
				String country = token.substring("{{флагификация|".length(), token.length() - "}}".length());
				if (country.contains("|")) {
					country = StringUtils.substringBefore(country, "|");
				}
				token = country.trim();
			}
			if (token.startsWith("[[") && token.endsWith("]]")) {
				String country = StringUtils.substringBetween(token, "[[", "]]");
				if (country.contains("|")) {
					country = StringUtils.substringBefore(country, "|");
				}
				token = country.trim();
			}
			if (DICTIONARY.containsKey(token.toLowerCase())) {
				token = DICTIONARY.get(token.toLowerCase());
			}
			result.add(token);
		}
		return result;
	}

}
