package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.WikidataBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMember;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMemberType;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
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

		public boolean update(Template titleTemplate) {
			boolean hasChanges = false;
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
				if (!hasLink(dictionary, strValue)) {
					addLink(dictionary, strValue);
					hasChanges = true;
				}
			}
			return hasChanges;
		}
	}

	private static enum Dictionary {

		ВИКИПЕДИЯ(Project.RUWIKISOURCE, StringUtils.EMPTY, StringUtils.EMPTY),

		ЛЕНТАПЕДИЯ(Project.RUWIKISOURCE, "Лентапедия/", StringUtils.EMPTY),

		ЛЕНТАПЕДИЯ_ПОЛНАЯ_ВЕРСИЯ(Project.RUWIKISOURCE, "Лентапедия/", "/Полная версия"),

		;

		public final String prefix;

		public final Project project;

		public final String suffix;

		private Dictionary(Project project, String prefix, String suffix) {
			this.project = project;
			this.prefix = prefix;
			this.suffix = suffix;
		}

		public String getPageTitle(String link) {
			return prefix + link + suffix;
		}
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
	private WikidataBot wikidataBot;

	private boolean collectFromPage(CategoryMember categoryMember, List<Circle> circles, Map<String, String> errors)
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

		Revision revision = ruWikisourceCache.queryLatestRevision(pageTitle);
		return collectFromPage(circles, dictionary, link, revision);
	}

	private boolean collectFromPage(List<Circle> circles, Dictionary dictionary, String link) throws Exception {
		final WikiCache wikiCache;
		if (dictionary.project == Project.RUWIKIPEDIA) {
			wikiCache = ruWikipediaCache;
		} else if (dictionary.project == Project.RUWIKIPEDIA) {
			wikiCache = ruWikisourceCache;
		} else {
			throw new IllegalArgumentException("Unknown project: " + dictionary.project);
		}

		String title = dictionary.getPageTitle(link);
		Revision revision = wikiCache.queryLatestRevision(title);
		return collectFromPage(circles, dictionary, link, revision);
	}

	private boolean collectFromPage(List<Circle> circles, Dictionary dictionary, final String link, Revision revision)
			throws Exception {
		ArticleFragment article = ruWikisourceBot.getXmlParser().parse(revision);
		final List<Template> titleTemplates = article.getAllTemplates().get("лентапедия");
		if (titleTemplates == null || titleTemplates.isEmpty()) {
			log.warn(revision.getPage() + " doesn't have 'лентапедия' template");
			return false;
		}
		Template titleTemplate = titleTemplates.get(0);

		boolean hasChanges = false;

		final Circle circle;
		List<Circle> matched = findByTitleTemplate(circles, titleTemplate);
		if (matched.size() > 1) {
			circles.removeAll(matched);
			circle = Circle.merge(matched);
			circles.add(circle);
			matched.clear();
			matched.add(circle);
			hasChanges = true;
		} else if (matched.isEmpty()) {
			circle = new Circle();
			circles.add(circle);
			matched.add(circle);
			hasChanges = true;
		} else {
			circle = matched.get(0);
		}

		if (!circle.hasLink(dictionary, link)) {
			circle.addLink(dictionary, link);
			hasChanges = true;
		}

		hasChanges = circle.update(titleTemplate) || hasChanges;
		return hasChanges;
	}

	private List<Circle> findByTitleTemplate(List<Circle> circles, Template titleTemplate) {
		List<Circle> matched = new LinkedList<Circle>();
		for (Circle circle : circles) {
			if (circle.matches(titleTemplate)) {
				matched.add(circle);
			}
		}
		return matched;
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
			collectFromPage(categoryMember, circles, errors);
		}

		boolean hasChanges = true;
		while (hasChanges) {
			hasChanges = false;
			for (int i = 0; i < circles.size(); i++) {
				Circle circle = circles.get(i);
				Set<Dictionary> copyKeys = new HashSet<Dictionary>(circle.links.keySet());
				for (Dictionary dictionary : copyKeys) {
					Set<String> copyValues = new LinkedHashSet<String>(circle.links.get(dictionary));
					for (String link : copyValues) {
						hasChanges = collectFromPage(circles, dictionary, link) || hasChanges;
					}
				}
			}
		}
	}

}
