package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.cache.XmlCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.WikidataBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMember;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMemberType;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

@Component
public class ImportLinksFromRuWikisourceTask implements Runnable {

	private static class Circle {

		public static Circle merge(List<Circle> circles) {
			Circle result = new Circle();
			for (Circle original : circles) {
				for (Entry<Dictionary, SortedSet<String>> links : original.links.entrySet()) {
					if (!result.links.containsKey(links.getKey())) {
						result.links.put(links.getKey(), new TreeSet<String>());
					}
					result.links.get(links.getKey()).addAll(links.getValue());
				}
			}

			return result;
		}

		final Map<Dictionary, SortedSet<String>> links = new HashMap<Dictionary, SortedSet<String>>();

		final Map<String, List<Content>> unknownValues = new HashMap<String, List<Content>>();

		public void addLink(Dictionary dictionary, String link) {
			if (dictionary == null) {
				throw new NullArgumentException("dictionary");
			}
			if (!links.containsKey(dictionary)) {
				links.put(dictionary, new TreeSet<String>());
			}
			links.get(dictionary).add(link);
		}

		public boolean hasLink(Dictionary dictionary, String title) {
			if (!links.containsKey(dictionary)) {
				return false;
			}
			return links.get(dictionary).contains(title);
		}

		public boolean matches(Template titleTemplate) {
			for (Entry<String, Dictionary> entry : MAPPED_FIELDS.entrySet()) {
				List<TemplatePart> parts = titleTemplate.getParameters(entry.getKey());
				for (TemplatePart part : parts) {
					String title = part.getValue().toWiki(true).trim();
					if (hasLink(entry.getValue(), title)) {
						return true;
					}
				}
			}

			return false;
		}

		public void update(Template titleTemplate) {
			for (TemplatePart templatePart : titleTemplate.getParameters()) {
				final String name = templatePart.getCanonicalName();
				final String strValue = templatePart.getValue().toWiki(true).trim();
				if (StringUtils.isEmpty(strValue)) {
					continue;
				}
				if (IGNORED_FIELDS.contains(name)) {
					continue;
				}
				Dictionary dictionary = MAPPED_FIELDS.get(name);
				if (dictionary == null) {
					if (!unknownValues.containsKey(name)) {
						unknownValues.put(name, new ArrayList<Content>());
					}
					unknownValues.get(name).add(templatePart.getValue());
					continue;
				}
				addLink(dictionary, strValue);
			}
		}
	}

	private static enum Dictionary {

		ВИКИПЕДИЯ,

		ЛЕНТАПЕДИЯ,

		ЛЕНТАПЕДИЯ_ПОЛНАЯ_ВЕРСИЯ,

		;
	}

	private static final Set<String> IGNORED_FIELDS = new HashSet<String>(Arrays.asList("id", "обновлено",
			"неоднозначность", "предыдущий", "следующий", "качество", "архив", "url", "полная версия"));

	private static final Log log = LogFactory.getLog(ImportLinksFromRuWikisourceTask.class);

	private static final Map<String, Dictionary> MAPPED_FIELDS = new HashMap<String, Dictionary>();

	static {
		MAPPED_FIELDS.put("википедия", Dictionary.ВИКИПЕДИЯ);
		MAPPED_FIELDS.put("лентапедия", Dictionary.ЛЕНТАПЕДИЯ);
	}

	@Autowired
	@Qualifier("ruWikipediaBot")
	private MediaWikiBot ruWikipediaBot;

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	@Autowired
	@Qualifier("ruWikisourceBot")
	private MediaWikiBot ruWikisourceBot;

	@Autowired
	@Qualifier("ruWikisourceCache")
	private WikiCache ruWikisourceCache;

	@Autowired
	@Qualifier("ruWikisourceXmlCache")
	private XmlCache ruWikisourceXmlCache;

	@Autowired
	private WikidataBot wikidataBot;

	private Circle findByTitleTemplate(List<Circle> circles, Template titleTemplate) {
		List<Circle> matched = new LinkedList<Circle>();
		for (Circle circle : circles) {
			if (circle.matches(titleTemplate)) {
				matched.add(circle);
			}
		}
		if (matched.size() > 1) {
			circles.removeAll(matched);
			Circle circle = Circle.merge(matched);
			circles.add(circle);
			matched.clear();
			matched.add(circle);
		}
		if (matched.isEmpty()) {
			return null;
		}
		return matched.get(0);
	}

	private void processPage(CategoryMember categoryMember, List<Circle> circles, Map<String, String> errors)
			throws Exception {

		final String pageTitle = categoryMember.getPageTitle();
		Dictionary dictionary = pageTitle.endsWith("/Полная версия") ? Dictionary.ЛЕНТАПЕДИЯ_ПОЛНАЯ_ВЕРСИЯ
				: Dictionary.ЛЕНТАПЕДИЯ;
		final String link;
		if (dictionary == Dictionary.ЛЕНТАПЕДИЯ_ПОЛНАЯ_ВЕРСИЯ) {
			link = StringUtils.substringBetween(pageTitle, "Лентапедия/", "/Полная версия");
		} else {
			link = StringUtils.substringAfter(pageTitle, "Лентапедия/");
		}

		Revision revision = ruWikisourceCache.queryLatestRevision(categoryMember.getPageId());
		ArticleFragment article = ruWikisourceBot.getXmlParser().parse(revision);
		final List<Template> titleTemplates = article.getAllTemplates().get("лентапедия");
		if (titleTemplates == null || titleTemplates.isEmpty()) {
			log.warn(revision.getPage() + " doesn't have 'лентапедия' template");
			return;
		}
		Template titleTemplate = titleTemplates.get(0);

		Circle circle = findByTitleTemplate(circles, titleTemplate);
		if (circle == null) {
			circle = new Circle();
			circles.add(circle);
		}
		circle.addLink(dictionary, link);
		circle.update(titleTemplate);
	}

	@Override
	public void run() {
		try {
			runImpl();
		} catch (Exception exc) {
			log.error(exc, exc);
		}
	}

	private void runImpl() throws Exception {
		List<Circle> circles = new ArrayList<Circle>();
		Map<String, String> errors = new LinkedHashMap<String, String>();

		for (CategoryMember categoryMember : ruWikisourceBot.queryCategoryMembers("Category:Лентапедия",
				CategoryMemberType.PAGE, Namespace.MAIN)) {
			if (!categoryMember.getPageTitle().startsWith("Лентапедия/")) {
				continue;
			}
			processPage(categoryMember, circles, errors);
		}

	}
}
