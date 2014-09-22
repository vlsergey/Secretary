package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.NullArgumentException;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.actions.QueryRevisionsByCategoryMembers.CmType;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.ProjectType;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiSnak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiStatement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityProperty;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Sitelink;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.StringValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikibaseEntityIdValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

@Component
public class ImportLinksFromRuWikisourceTask implements Runnable {

	private class Circle {

		final Map<Dictionary, SortedSet<Link>> links = new HashMap<Dictionary, SortedSet<Link>>();

		final Map<String, List<Content>> unknownValues = new HashMap<String, List<Content>>();

		public boolean addLink(Link link) {
			if (link == null)
				throw new NullArgumentException("link");

			final Dictionary dictionary = link.dictionary;
			if (!links.containsKey(dictionary)) {
				links.put(dictionary, new TreeSet<Link>());
			}
			return links.get(dictionary).add(link);
		}

		public boolean hasLink(Dictionary dictionary, String link) {
			if (!links.containsKey(dictionary)) {
				return false;
			}
			return links.get(dictionary).stream().anyMatch(x -> link.equals(x.articleName));
		}

		public boolean isSimple() {
			return links.values().stream().allMatch(values -> values.size() == 1)
					&& links.values().stream().flatMap(x -> x.stream()).allMatch(x -> x.exists);
		}

		public boolean matches(Template titleTemplate) {
			for (Entry<String, Dictionary> entry : MAPPED_FIELDS.entrySet()) {
				final Dictionary dictionary = entry.getValue();
				if (links.containsKey(dictionary)) {
					List<TemplatePart> parts = titleTemplate.getParameters(entry.getKey());
					for (TemplatePart part : parts) {
						String title = part.getValue().toWiki(true).trim();
						if (hasLink(dictionary, title)) {
							return true;
						}
					}
				}
			}

			return false;
		}

		public boolean matchesWikidataId(EntityId wikidataId) {
			if (links.get(Dictionary.ВИКИПЕДИЯ) == null) {
				return false;
			}
			return links.get(Dictionary.ВИКИПЕДИЯ).stream() //
					.anyMatch(link -> wikidataId.equals(link.entityId));
		}

		@Override
		public String toString() {
			return ToStringBuilder.reflectionToString(this, ToStringStyle.MULTI_LINE_STYLE);
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
					final Link link = getLinkByPageTitle(dictionary, dictionary.getPageTitle(strValue));
					if (link != null) {
						addLink(link);
						hasChanges = true;
					}
				}
			}
			return hasChanges;
		}
	}

	private static enum Dictionary {

		ВИКИПЕДИЯ(Project.RUWIKIPEDIA, StringUtils.EMPTY, StringUtils.EMPTY, null, null, x -> x, x -> StringUtils.EMPTY),

		ЛЕНТАПЕДИЯ(Project.RUWIKISOURCE, "Лентапедия/", StringUtils.EMPTY, "лентапедия", EntityId.item(17290934),
				x -> "Лентапедия / " + x, x -> "Статья «" + x + "» в энциклопедии «Лентапедия»"),

		ЛЕНТАПЕДИЯ_ПОЛНАЯ_ВЕРСИЯ(Project.RUWIKISOURCE, "Лентапедия/", "/Полная версия", "лентапедия", EntityId
				.item(17311605), x -> "Лентапедия (полная) / " + x, x -> "Полная версия статьи «" + x
				+ "» в энциклопедии «Лентапедия»"),

		;

		public static Dictionary getByWikidataId(EntityId wikidataId) {
			for (Dictionary dictionary : values()) {
				if (dictionary.wikidataId.equals(wikidataId)) {
					return dictionary;
				}
			}
			return null;
		}

		public final String prefix;

		public final Project project;

		public final String suffix;

		public final String titleTemplate;

		public final Function<String, String> wikidataDescriptionF;

		public final EntityId wikidataId;

		public final Function<String, String> wikidataTitleF;

		private Dictionary(Project project, String prefix, String suffix, String titleTemplate, EntityId wikidataId,
				final Function<String, String> wikidataTitleF, final Function<String, String> wikidataDescriptionF) {

			if (project == null)
				throw new NullArgumentException("project");
			if (wikidataTitleF == null)
				throw new NullArgumentException("wikidataTitleF");
			if (wikidataDescriptionF == null)
				throw new NullArgumentException("wikidataDescriptionF");

			this.project = project;
			this.prefix = prefix;
			this.suffix = suffix;
			this.titleTemplate = titleTemplate;
			this.wikidataId = wikidataId;
			this.wikidataTitleF = wikidataTitleF;
			this.wikidataDescriptionF = wikidataDescriptionF;
		}

		public String getLink(String pageTitle) {
			String result = pageTitle;
			if (StringUtils.isNotEmpty(prefix)) {
				if (!result.startsWith(prefix)) {
					throw new IllegalArgumentException("Title '" + pageTitle + "' does not start with '" + prefix + "'");
				}
				result = StringUtils.substringAfter(result, prefix);
			}
			if (StringUtils.isNotEmpty(suffix)) {
				if (!result.endsWith(suffix)) {
					throw new IllegalArgumentException("Title '" + pageTitle + "' does not end with '" + suffix + "'");
				}
				result = StringUtils.substringBeforeLast(result, suffix);
			}
			return result;
		}

		public String getPageTitle(String link) {
			return prefix + link + suffix;
		}
	}

	private class Link implements Comparable<Link> {

		Entity apiEntity;

		final String articleName;

		final Dictionary dictionary;

		EntityId entityId;

		boolean exists = true;

		Revision latestRevision = null;

		final String pageTitle;

		private Link(final Dictionary dictionary, final String pageTitle, final String articleName) {
			if (dictionary == null)
				throw new NullArgumentException("dictionary");
			if (pageTitle == null)
				throw new NullArgumentException("pageTitle");
			if (articleName == null)
				throw new NullArgumentException("articleName");

			this.dictionary = dictionary;
			this.pageTitle = pageTitle;
			this.articleName = articleName;
		}

		@Override
		public int compareTo(Link link) {
			return this.articleName.compareTo(link.articleName);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Link other = (Link) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;
			if (dictionary != other.dictionary)
				return false;
			if (articleName == null) {
				if (other.articleName != null)
					return false;
			} else if (!articleName.equals(other.articleName))
				return false;
			return true;
		}

		private ImportLinksFromRuWikisourceTask getOuterType() {
			return ImportLinksFromRuWikisourceTask.this;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + getOuterType().hashCode();
			result = prime * result + ((dictionary == null) ? 0 : dictionary.hashCode());
			result = prime * result + ((articleName == null) ? 0 : articleName.hashCode());
			return result;
		}

		@Override
		public String toString() {
			return "Link [" + dictionary + ";" + pageTitle + ";" + entityId + "; " + (exists ? "exists" : "NOT EXISTS")
					+ "]";
		}
	}

	private static final Set<String> IGNORED_FIELDS = new HashSet<String>(Arrays.asList("id", "обновлено",
			"неоднозначность", "предыдущий", "следующий", "качество", "архив", "url", "полная версия"));

	private static final String ITEM_encyclopedic_article = "Q17329259";

	private static final Log log = LogFactory.getLog(ImportLinksFromRuWikisourceTask.class);

	private static final Map<String, Dictionary> MAPPED_FIELDS = new HashMap<String, Dictionary>();

	private static final String SUMMARY = "Import links from Russian Wikisource dictionaries";

	static {
		MAPPED_FIELDS.put("википедия", Dictionary.ВИКИПЕДИЯ);
		MAPPED_FIELDS.put("лентапедия", Dictionary.ЛЕНТАПЕДИЯ);
	}

	private final Map<EntityId, ApiEntity> entitiesByIdCache = new HashMap<>();

	private final Map<String, ApiEntity> entitiesByPageTitleCache = new HashMap<>();

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

	private boolean collectFromPage(List<Circle> circles, Link link) throws Exception {
		if (link == null)
			throw new NullArgumentException("link");

		Revision latestRevision = link.latestRevision;
		if (latestRevision == null) {
			final WikiCache wikiCache;
			if (link.dictionary.project == Project.RUWIKIPEDIA) {
				wikiCache = ruWikipediaCache;
			} else if (link.dictionary.project == Project.RUWIKISOURCE) {
				wikiCache = ruWikisourceCache;
			} else {
				throw new IllegalArgumentException("Unknown project: " + link.dictionary.project);
			}

			latestRevision = wikiCache.queryLatestRevision(link.pageTitle);
			link.latestRevision = latestRevision;
			if (latestRevision == null) {
				link.exists = false;
			}
		}
		return collectFromPage(circles, link, latestRevision);
	}

	private boolean collectFromPage(List<Circle> circles, final Link link, Revision revision) throws Exception {
		if (link == null)
			throw new NullArgumentException("link");

		boolean hasChanges = false;

		List<Circle> matchedByLink = findByLink(circles, link);
		hasChanges = mergeOrCreate(circles, null, matchedByLink) || hasChanges;
		Circle circle = matchedByLink.get(0);

		if (link.dictionary.project == Project.RUWIKIPEDIA && link.entityId != null && link.apiEntity != null) {
			List<Circle> matched = findByWikidataId(circles, link.entityId);
			hasChanges = mergeOrCreate(circles, circle, matched) || hasChanges;
			circle = matched.get(0);
			hasChanges = circle.addLink(link) || hasChanges;

			// get wikidata item
			Entity apiEntity = link.apiEntity;
			// get linked dictionaries

			for (Statement describedBy : apiEntity.getClaims(Properties.DESCRIBED_BY)) {
				EntityId dictionaryId = describedBy.getMainSnak().getWikibaseEntityIdValue().getEntityId();
				Dictionary claimDictionary = Dictionary.getByWikidataId(dictionaryId);
				if (claimDictionary != null) {
					EntityId linkWikidataId = describedBy.getQualifiers(Properties.STATED_IN)[0]
							.getWikibaseEntityIdValue().getEntityId();

					Link newLink = getLinkByWikidataId(claimDictionary, linkWikidataId);
					if (newLink != null) {
						hasChanges = circle.addLink(newLink) || hasChanges;
					}
				}
			}
		}

		if (link.dictionary.project == Project.RUWIKISOURCE) {
			// get Wikipedia link from Wikidata
			if (link.apiEntity != null) {
				// get main topic
				for (Statement mainTopicClaim : link.apiEntity.getClaims(Properties.MAIN_TOPIC)) {
					EntityId wikidataMainTopicLink = mainTopicClaim.getMainSnak().getWikibaseEntityIdValue()
							.getEntityId();

					List<Circle> matched = findByWikidataId(circles, wikidataMainTopicLink);
					hasChanges = mergeOrCreate(circles, circle, matched) || hasChanges;
					circle = matched.get(0);
					final Link newLink = getLinkByWikidataId(Dictionary.ВИКИПЕДИЯ, wikidataMainTopicLink);
					hasChanges = circle.addLink(link) || hasChanges;
					hasChanges = circle.addLink(newLink) || hasChanges;
				}
			}
		}

		if (link.dictionary.project == Project.RUWIKISOURCE && StringUtils.isNotEmpty(link.dictionary.titleTemplate)) {
			ArticleFragment article = ruWikisourceBot.getXmlParser().parse(revision);

			final List<Template> titleTemplates = article.getTemplates(link.dictionary.titleTemplate);
			if (titleTemplates == null || titleTemplates.isEmpty()) {
				log.warn(revision.getPage() + " doesn't have «" + link.dictionary.titleTemplate + "» template");
				return false;
			}
			Template titleTemplate = titleTemplates.get(0);

			List<Circle> matched = findByTitleTemplate(circles, titleTemplate);
			hasChanges = mergeOrCreate(circles, circle, matched) || hasChanges;
			circle = matched.get(0);
			hasChanges = circle.addLink(link) || hasChanges;
			hasChanges = circle.update(titleTemplate) || hasChanges;
		}

		return hasChanges;
	}

	private List<Circle> findByLink(List<Circle> circles, Link link) {
		return circles
				.stream()
				.filter(circle -> circle.links.containsKey(link.dictionary)
						&& circle.links.get(link.dictionary).contains(link))//
				.collect(Collectors.toList());
	}

	private List<Circle> findByTitleTemplate(List<Circle> circles, Template titleTemplate) {
		return circles.stream()//
				.filter(circle -> circle.matches(titleTemplate))//
				.collect(Collectors.toList());
	}

	private List<Circle> findByWikidataId(List<Circle> circles, EntityId wikidataId) {
		return circles.stream()//
				.filter(circle -> circle.matchesWikidataId(wikidataId))//
				.collect(Collectors.toList());
	}

	Link getLinkByPageTitle(Dictionary dictionary, String pageTitle) {
		Link link = new Link(dictionary, pageTitle, dictionary.getLink(pageTitle));

		Entity apiEntity = getWikidataEntity(dictionary.project, pageTitle);
		if (apiEntity != null) {
			Sitelink sitelink = apiEntity.getSiteLink(dictionary.project.getCode());
			if (sitelink == null) {
				throw new IllegalStateException("Wikidata entry was found by sitelink [" + dictionary.project + "/"
						+ pageTitle + "], but sitelink is missing: " + apiEntity);
			}

			link.apiEntity = apiEntity;
			link.entityId = apiEntity.getId();
		} else {
			link.apiEntity = null;
			link.entityId = null;
		}
		return link;
	}

	Link getLinkByWikidataId(Dictionary dictionary, EntityId entityId) {
		Entity apiEntity = getWikidataEntity(entityId);
		Link link;
		Sitelink sitelink = apiEntity.getSiteLink(dictionary.project.getCode());
		if (sitelink == null) {
			// missing wikipedia or wikisource article
			link = new Link(dictionary, dictionary.getPageTitle(entityId), entityId);
			link.exists = false;
		} else {
			link = new Link(dictionary, sitelink.getTitle(), dictionary.getLink(sitelink.getTitle()));
		}

		link.apiEntity = apiEntity;
		link.entityId = entityId;
		return link;
	}

	private Entity getWikidataEntity(EntityId entityId) {
		ApiEntity result = entitiesByIdCache.get(entityId);
		if (result == null) {
			result = wikidataBot.wgGetEntity(entityId, EntityProperty.claims, EntityProperty.descriptions,
					EntityProperty.labels, EntityProperty.sitelinks);
			entitiesByIdCache.put(entityId, result);
		}
		return result;
	}

	private Entity getWikidataEntity(Project project, String pageTitle) {
		final String key = project.getCode() + "/" + pageTitle;
		ApiEntity result = entitiesByPageTitleCache.get(key);
		if (result == null) {
			result = wikidataBot.wgGetEntityBySitelink(project.getCode(), pageTitle, EntityProperty.claims,
					EntityProperty.descriptions, EntityProperty.labels, EntityProperty.sitelinks);
			if (result != null) {
				entitiesByPageTitleCache.put(key, result);
				entitiesByIdCache.put(result.getId(), result);
			}
		}
		return result;
	}

	public Circle merge(List<Circle> circles) {
		Circle result = new Circle();
		for (Circle original : circles) {
			for (Entry<Dictionary, SortedSet<Link>> links : original.links.entrySet()) {
				if (!result.links.containsKey(links.getKey())) {
					result.links.put(links.getKey(), new TreeSet<Link>());
				}
				result.links.get(links.getKey()).addAll(links.getValue());
			}
		}

		return result;
	}

	private boolean mergeOrCreate(List<Circle> circles, Circle mainCircle, List<Circle> matched) {
		boolean hasChanges = false;

		if (mainCircle != null && !matched.contains(mainCircle)) {
			matched.add(mainCircle);
		}

		final Circle circle;
		if (matched.size() > 1) {
			circles.removeAll(matched);
			circle = merge(matched);
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

		matched.clear();
		matched.add(circle);

		return hasChanges;
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

		for (Revision revision : ruWikisourceCache.queryByCaterogyMembers("Category:Лентапедия",
				new Namespace[] { Namespace.MAIN }, CmType.page)) {
			if (!revision.getPage().getTitle().startsWith("Лентапедия/")) {
				continue;
			}
			// if (!revision.getPage().getTitle().startsWith("Лентапедия/A")) {
			// continue;
			// }

			final String pageTitle = revision.getPage().getTitle();
			Dictionary dictionary = pageTitle.endsWith("/Полная версия") ? Dictionary.ЛЕНТАПЕДИЯ_ПОЛНАЯ_ВЕРСИЯ
					: Dictionary.ЛЕНТАПЕДИЯ;

			Link link = getLinkByPageTitle(dictionary, pageTitle);
			link.latestRevision = revision;
			collectFromPage(circles, link, revision);
		}

		boolean hasChanges = true;
		while (hasChanges) {

			hasChanges = false;
			for (int i = 0; i < circles.size(); i++) {
				Circle circle = circles.get(i);
				Set<Dictionary> copyKeys = new HashSet<Dictionary>(circle.links.keySet());
				for (Dictionary dictionary : copyKeys) {
					Set<Link> copyValues = new LinkedHashSet<>(circle.links.get(dictionary));
					for (Link link : copyValues) {
						hasChanges = collectFromPage(circles, link) || hasChanges;
					}
				}
			}
		}

		for (Circle circle : circles) {
			if (circle.isSimple()) {
				updateLinks(circle);
			} else {
				System.out.println("Too complex to update: \n" + circle);
			}
		}
	}

	private void updateLink(Circle circle, final Link link) {
		final Dictionary dictionary = link.dictionary;
		final Project project = dictionary.project;

		final String siteCode = project.getCode();
		final String langCode = project.getLanguageCode();
		final String newEntityTitle = dictionary.wikidataTitleF.apply(link.articleName);
		final String newEntityDescription = dictionary.wikidataDescriptionF.apply(link.articleName);

		Entity apiEntity = link.apiEntity;
		if (apiEntity == null) {
			apiEntity = new ApiEntity(new JSONObject());
		}

		final JSONObject newData = new JSONObject();

		if (!apiEntity.hasLabel(langCode) && StringUtils.isNotBlank(newEntityTitle)) {
			final JSONObject labels = new JSONObject();
			labels.put("language", langCode);
			labels.put("value", newEntityTitle);
			newData.put("labels", Collections.singletonMap(langCode, labels));
		}

		if (!apiEntity.hasDescription(langCode) && StringUtils.isNotBlank(newEntityDescription)) {
			final JSONObject descriptions = new JSONObject();
			descriptions.put("language", langCode);
			descriptions.put("value", newEntityDescription);
			newData.put("descriptions", Collections.singletonMap(langCode, descriptions));
		}

		if (!apiEntity.hasSitelink(siteCode) && link.exists) {
			final JSONObject sitelink = new JSONObject();
			sitelink.put("site", siteCode);
			sitelink.put("title", link.pageTitle);
			newData.put("sitelinks", Collections.singletonMap(siteCode, sitelink));
		}

		if (project.getType() == ProjectType.wiki) {

			// set links to Wikisource project
			Statement[] claims = apiEntity.getClaims(Properties.DESCRIBED_BY);
			for (Link sublink : circle.links.values().stream().flatMap(x -> x.stream())
					.filter(x -> x.dictionary.project.getType() == ProjectType.wikisource) //
					.filter(x -> x.dictionary.wikidataId != null) //
					.filter(x -> x.entityId != null) //
					.collect(Collectors.toList())) {
				if (!Arrays.stream(claims).anyMatch(x -> x.isWikibaseEntityIdValue(sublink.dictionary.wikidataId))) {
					// need to create new
					ApiStatement apiStatement = ApiStatement.newStatement(Properties.DESCRIBED_BY,
							sublink.dictionary.wikidataId);

					{
						ApiSnak qualifier = new ApiSnak();
						qualifier.setProperty(Properties.STATED_IN);
						qualifier.setSnakType(SnakType.value);
						qualifier.setDataType(DataType.WIKIBASE_ITEM);
						qualifier.setDatavalue(new WikibaseEntityIdValue(sublink.entityId));
						apiStatement.addQualifier(qualifier);
					}
					{
						ApiSnak qualifier = new ApiSnak();
						qualifier.setProperty(Properties.TITLE);
						qualifier.setSnakType(SnakType.value);
						qualifier.setDataType(DataType.STRING);
						qualifier.setDatavalue(new StringValue(sublink.articleName));
						apiStatement.addQualifier(qualifier);
					}

					ApiEntity.putProperty(newData, apiStatement);
				}
			}
		}

		if (project.getType() == ProjectType.wikisource) {
			if (!apiEntity.hasClaims(Properties.MAIN_TOPIC)) {
				Link wikipediaLink = circle.links.get(Dictionary.ВИКИПЕДИЯ) != null ? circle.links.get(
						Dictionary.ВИКИПЕДИЯ).first() : null;
				if (wikipediaLink != null && wikipediaLink.entityId != null) {
					ApiEntity.putProperty(newData,
							ApiStatement.newStatement(Properties.MAIN_TOPIC, wikipediaLink.entityId));
				}
			}

			if (!apiEntity.hasClaims(Properties.PART_OF)) {
				if (link.dictionary.wikidataId != null) {
					ApiEntity.putProperty(newData,
							ApiStatement.newStatement(Properties.PART_OF, link.dictionary.wikidataId));
				}
			}

			if (!apiEntity.hasClaims(Properties.INSTANCE_OF)) {
				ApiEntity.putProperty(newData,
						ApiStatement.newStatement(Properties.INSTANCE_OF, ITEM_encyclopedic_article));
			}

			if (!apiEntity.hasClaims(Properties.TITLE)) {
				if (StringUtils.isNotBlank(link.articleName)) {
					ApiEntity.putProperty(newData, ApiStatement.newStatement(Properties.TITLE, link.articleName));
				}
			}
		}

		if (newData.length() != 0) {
			if (link.apiEntity == null) {
				apiEntity = wikidataBot.wgCreateEntity(newData, SUMMARY);
				link.apiEntity = apiEntity;
				link.entityId = apiEntity.getId();
			} else {
				apiEntity = wikidataBot.wgEditEntity(apiEntity, newData, SUMMARY);
				link.apiEntity = apiEntity;
				link.entityId = apiEntity.getId();
			}
		}
	}

	private void updateLinks(Circle circle) {
		for (int i = 0; i < 3; i++) {
			for (Map.Entry<Dictionary, ? extends Collection<Link>> entry : circle.links.entrySet()) {
				if (entry.getKey().project.getType() == ProjectType.wikisource) {
					updateLink(circle, entry.getValue().iterator().next());
				}
			}
			for (Map.Entry<Dictionary, ? extends Collection<Link>> entry : circle.links.entrySet()) {
				if (entry.getKey().project.getType() == ProjectType.wiki) {
					updateLink(circle, entry.getValue().iterator().next());
				}
			}
		}
	}
}
