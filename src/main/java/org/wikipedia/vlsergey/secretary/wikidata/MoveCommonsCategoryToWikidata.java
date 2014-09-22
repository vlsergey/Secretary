package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.dom.Text;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiStatement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityProperty;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class MoveCommonsCategoryToWikidata implements Runnable {

	private static final EntityId PROPERTY = EntityId.property(373);
	private static final String TEMPLATE = "commonscat";
	private static final EntityId TEMPLATE_ID = EntityId.item(48029);

	@Autowired
	@Qualifier("commonsCache")
	private WikiCache commonsCache;

	@Autowired
	@Qualifier("ruWikipediaBot")
	private MediaWikiBot ruWikipediaBot;

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	@Autowired
	private WikidataBot wikidataBot;

	private void fillFromWikidata(Entity entity, EntityId property, Set<String> result) {
		for (Statement statement : entity.getClaims(property)) {
			switch (statement.getMainSnak().getSnakType()) {
			case novalue:
				continue;
			case somevalue:
				continue;
			case value:
				result.add(statement.getStringValue().getValue().trim());
			}
		}
	}

	private void fillToWikidata(Set<String> source, EntityId property, JSONObject result) {
		if (!source.isEmpty()) {
			for (String newValue : source) {
				ApiStatement statement = ApiStatement.newStatement(property, newValue);
				ApiEntity.putProperty(result, statement);
			}
		}
	}

	private String normalizeCategoryName(String value) throws Exception {
		value = value.replace('_', ' ');
		lastCharCheck: while (true) {
			switch (value.charAt(value.length() - 1)) {
			case 0:
			case 8206:
				value = value.substring(0, value.length() - 1);
				continue;
			default:
				break lastCharCheck;
			}
		}
		value = value.trim();
		if (value.startsWith("Category:")) {
			value = value.substring("Category:".length());
		}
		value = Character.toUpperCase(value.charAt(0)) + value.substring(1);

		// check if soft redirect
		Revision commonsCategoryRevision = commonsCache.queryLatestRevision("Category:" + value);
		if (commonsCategoryRevision != null && StringUtils.isNotBlank(commonsCategoryRevision.getXml())) {
			ArticleFragment commonsFragment = commonsCache.getMediaWikiBot().getXmlParser()
					.parse(commonsCategoryRevision);
			for (Template softRedirectTemplate : commonsFragment.getTemplates("category redirect")) {
				TemplatePart templatePart = softRedirectTemplate.getParameter(0);
				if (templatePart != null && templatePart.getName() == null && templatePart.getValue() != null) {
					value = templatePart.getValue().toWiki(true).trim();
				}
			}
		}
		return value;
	}

	private String process(Revision revision) throws Exception {
		StringBuilder result = new StringBuilder();

		Set<String> fromPedia = new LinkedHashSet<>();

		ArticleFragment fragment = ruWikipediaBot.getXmlParser().parse(revision);
		if (fragment.getTemplates(TEMPLATE.toLowerCase()).isEmpty()) {
			return null;
		}

		for (Template template : fragment.getTemplates(TEMPLATE.toLowerCase())) {
			if (template.getParameters().size() >= 1 && template.getParameter(0) != null) {
				final Content value = template.getParameter(0).getValue();
				if (value != null) {
					String inRuWiki = value.toWiki(true);
					inRuWiki = normalizeCategoryName(inRuWiki);
					fromPedia.add(inRuWiki);
				}
			}
			String subject = null;
			if (template.getParameters().size() >= 2 && template.getParameter(1) != null) {
				subject = template.getParameter(1).getValue().toWiki(true).trim();
			}

			template.getParameters().clear();
			template.setTitle(new Text("Навигация"));
			if (StringUtils.isNotBlank(subject) && !StringUtils.equals(subject, revision.getPage().getTitle())) {
				template.getParameters().add(new TemplatePart(new Text("Тема"), new Text(subject)));
			}
		}

		if (!fromPedia.isEmpty()) {

			ApiEntity entity = wikidataBot.wgGetEntityBySitelink("ruwiki", revision.getPage().getTitle(),
					EntityProperty.claims);

			if (entity == null) {
				JSONObject data = new JSONObject();

				{
					final JSONObject labels = new JSONObject();
					labels.put("language", "ru");
					labels.put("value", revision.getPage().getTitle());
					data.put("labels", Collections.singletonMap("ru", labels));
				}
				{
					final JSONObject sitelink = new JSONObject();
					sitelink.put("site", "ruwiki");
					sitelink.put("title", revision.getPage().getTitle());
					data.put("sitelinks", Collections.singletonMap("ruwiki", sitelink));
				}
				entity = wikidataBot.wgCreateEntity(data);
				entity = wikidataBot.wgGetEntityBySitelink("ruwiki", revision.getPage().getTitle(),
						EntityProperty.claims);
				if (entity == null) {
					throw new RuntimeException();
				}
				// return;
			}

			Set<String> fromData = new HashSet<>();
			fillFromWikidata(entity, PROPERTY, fromData);

			final JSONObject newData = new JSONObject();

			if (!fromData.isEmpty() && !fromData.containsAll(fromPedia)) {
				/*
				 * we have some non-equal values in Wikipedia and Wikidata, do
				 * not touch
				 */
				result.append("| [[" + revision.getPage().getTitle() + "]] || " + toCommonsCategory(fromPedia) + "\n| "
						+ toCommonsCategory(fromData) + "\n| [[:d:" + entity.getId() + "|" + entity.getId() + "]]\n");
				result.append("|-\n");
				return result.toString();
			}

			fromPedia.removeAll(fromData);
			if (!fromPedia.isEmpty()) {
				fillToWikidata(fromPedia, PROPERTY, newData);
			}

			if (newData.length() != 0) {
				wikidataBot.wgEditEntity(entity, newData, "Move [[" + TEMPLATE_ID.toString() + "]] from ruwiki: "
						+ fromPedia);
			}
		}

		ruWikipediaBot.writeContent(revision, fragment.toWiki(false), "Move [[Шаблон:" + TEMPLATE
				+ "]] parameters to Wikidata", true);
		return null;
	}

	@Override
	public void run() {

		ExecutorService executorService = Executors.newFixedThreadPool(3 * 4);
		List<Future<String>> tasks = new ArrayList<>();

		for (Revision revision : ruWikipediaCache.queryByEmbeddedIn("Шаблон:" + TEMPLATE, Namespace.NSS_MAIN)) {
			tasks.add(executorService.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					return process(revision);
				}
			}));
		}

		StringBuilder report = new StringBuilder("{| class=\"wikitable sortable\"\n");
		report.append("! Статья\n");
		report.append("! Локальное значение\n");
		report.append("! Значение на Викиданных\n");
		report.append("! Элемент Викиданных\n");
		report.append("|-\n");

		for (Future<String> task : tasks) {
			try {
				final String result = task.get();
				if (result != null) {
					report.append(result);
				}
			} catch (Exception e) {
				e.printStackTrace();
				// throw new RuntimeException(e);
			}
		}

		report.append("|}\n");

		ruWikipediaBot.writeContent("User:Secretary/commonscat", null, report.toString(), null, "", true, false);
		executorService.shutdown();
	}

	private String toCommonsCategory(Collection<String> list) {

		if (list.isEmpty())
			return "";

		if (list.size() == 1) {
			final String single = list.iterator().next();
			return "[[commons:Category:" + single + "|" + single + "]]";
		}

		StringBuilder result = new StringBuilder();
		for (String categoryName : list) {
			result.append("\n* [[commons:Category:" + categoryName + "|" + categoryName + "]]");
		}
		return result.toString();
	}
}
