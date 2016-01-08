package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableInt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Component
public class CalculateCountries implements Runnable {

	public static String[] FIELDS = { "страна", "гражданство", "страна", };

	public static String[] TEMPLATES = {
	// "Космонавт", "Музыкант", "Мультсериал",
	// "Писатель", "Религиозный деятель", "Священник",
	// "Создатель комиксов",
	// "Теннисист",
	"Учёный",
	// "Философ", "Фотомодель", "Фильм"
	};

	@Autowired
	private CountriesHelper countriesHelper;

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	@Override
	public void run() {

		final Map<String, MutableInt> unparsed = new HashMap<>();
		final Map<List<String>, MutableInt> parsed = new HashMap<>();

		for (String template : TEMPLATES) {
			for (Revision revision : ruWikipediaCache.queryByEmbeddedIn("Шаблон:" + template, Namespace.NSS_MAIN)) {
				try {
					ArticleFragment fragment = ruWikipediaCache.getMediaWikiBot().getXmlParser().parse(revision);
					for (Template articleTemplate : fragment.getTemplates(template.toLowerCase())) {
						for (String field : FIELDS) {
							final Content value = articleTemplate.getParameterValue(field);
							if (value != null) {
								final String strValue = value.toWiki(true).trim();

								{
									MutableInt i = unparsed.get(strValue);
									if (i == null) {
										i = new MutableInt(0);
										unparsed.put(strValue, i);
									}
									i.increment();
								}

								try {
									final List<String> values = countriesHelper.normalize(strValue);
									if (StringUtils.isNotEmpty(strValue)) {
										MutableInt i = parsed.get(values);
										if (i == null) {
											i = new MutableInt(0);
											parsed.put(values, i);
										}
										i.increment();
									}
								} catch (Exception exc) {
									exc.printStackTrace();
								}
							}
						}
					}
				} catch (Exception exc) {
					exc.printStackTrace();
				}
			}
		}

		{
			List<String> values = new ArrayList<>(unparsed.keySet());
			Collections
					.sort(values, (x, y) -> -Integer.compare(unparsed.get(x).intValue(), unparsed.get(y).intValue()));

			StringBuilder stringBuilder = new StringBuilder();
			int count = 0;
			for (String value : values) {
				stringBuilder.append("* " + unparsed.get(value) + " — <nowiki>" + value + "</nowiki>\n");
				count++;
				if (count > 100) {
					break;
				}
			}
			ruWikipediaCache.getMediaWikiBot().writeContent(
					"User:" + ruWikipediaCache.getMediaWikiBot().getLogin() + "/countries/unparsed", null,
					stringBuilder.toString(), null, "update", true, false);
		}

		{
			List<List<String>> values = new ArrayList<>(parsed.keySet());
			Collections.sort(values, (x, y) -> -Integer.compare(parsed.get(x).intValue(), parsed.get(y).intValue()));

			StringBuilder stringBuilder = new StringBuilder();
			int count = 0;
			for (List<String> value : values) {
				stringBuilder.append("* " + parsed.get(value) + " — ");
				for (String country : value) {
					stringBuilder.append(country);
					stringBuilder.append("; ");
				}
				stringBuilder.append("\n");
				count++;
				if (count > 100) {
					break;
				}
			}
			ruWikipediaCache.getMediaWikiBot().writeContent(
					"User:" + ruWikipediaCache.getMediaWikiBot().getLogin() + "/countries", null,
					stringBuilder.toString(), null, "update", true, false);
		}
	}
}
