package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiStatement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityProperty;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.StringValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.TimeValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikibaseEntityIdValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class MoveDataToWikidata implements Runnable {

	private enum Action {
		append(true),

		remove_from_wikipedia(true),

		replace(true),

		report_difference(false),

		;

		final boolean removeFromWikipedia;

		private Action(final boolean removeFromWikipedia) {
			this.removeFromWikipedia = removeFromWikipedia;
		}
	}

	static class PropertyDescriptor {
		final DataType dataType;
		final EntityId property;
		final String templateParameter;
		final Function<String, List<DataValue>> toWikidata;

		public PropertyDescriptor(String templateProperty, DataType dataType, long propertyid) {
			this.templateParameter = templateProperty;
			this.dataType = dataType;
			this.property = EntityId.property(propertyid);
			this.toWikidata = x -> Collections.singletonList(new StringValue(x));
		}

		public PropertyDescriptor(String templateProperty, DataType dataType, long propertyid,
				Function<String, List<DataValue>> toWikidata) {
			this.templateParameter = templateProperty;
			this.dataType = dataType;
			this.property = EntityId.property(propertyid);
			this.toWikidata = toWikidata;
		}

		public Action getAction(Collection<DataValue> wikipedia, Collection<DataValue> wikidata) {
			if (wikipedia.isEmpty()) {
				return Action.remove_from_wikipedia;
			}
			if (wikidata.isEmpty()) {
				return Action.append;
			}
			if (wikidata.containsAll(wikipedia)) {
				return Action.remove_from_wikipedia;
			}
			return Action.report_difference;
		}
	}

	private static final Logger log = LoggerFactory.getLogger(MoveDataToWikidata.class);

	private static final DataValue NOVALUE = new StringValue("(novalue)");

	private static final Set<String> NOVALUES = new HashSet<>(Arrays.asList(
	// "notpl", "noipni"
			));

	private static final String TEMPLATE = "Учёный";

	@Autowired
	private CountriesHelper countriesHelper;

	private final List<PropertyDescriptor> parametersToMove = new ArrayList<>();

	@Autowired
	@Qualifier("ruWikipediaBot")
	private MediaWikiBot ruWikipediaBot;

	@Autowired
	@Qualifier("ruWikipediaCache")
	private WikiCache ruWikipediaCache;

	@Autowired
	private TimeHelper timeHelper;

	@Autowired
	private WikidataBot wikidataBot;

	{
		// parametersToMove.add(new PropertyDescriptor("Флаг", 41));
		// parametersToMove.add(new PropertyDescriptor("Герб", 94));
		// parametersToMove.add(new PropertyDescriptor("Категория в Commons",
		// 373));
		// parametersToMove.add(new PropertyDescriptor("Телефонный код", 473));
		// parametersToMove.add(new PropertyDescriptor("ncbi", 685));
		// parametersToMove.add(new PropertyDescriptor("ОКАТО", 721));
		// parametersToMove.add(new PropertyDescriptor("ОКТМО", 764));
		// parametersToMove.add(new PropertyDescriptor("itis", 815));
		// parametersToMove.add(new PropertyDescriptor("eol", 830));
		// parametersToMove.add(new PropertyDescriptor("Сайт", 856));
		// parametersToMove.add(new PropertyDescriptor("ipni", 961, x ->
		// x.contains("-") ? x : x + "-1"));
		// parametersToMove.add(new PropertyDescriptor("tpl", 1070));
		// parametersToMove.add(new PropertyDescriptor("tpl", 1070));

		parametersToMove.add(new PropertyDescriptor("Дата рождения", DataType.TIME, 569, x -> timeHelper.parse(x)) {

			@Override
			public Action getAction(Collection<DataValue> wikipedia, Collection<DataValue> wikidata) {
				if (wikipedia.size() > 1) {
					return Action.report_difference;
				}
				if (wikipedia.size() == 1 && wikidata.size() == 1) {
					TimeValue a = (TimeValue) wikipedia.iterator().next();
					TimeValue b = (TimeValue) wikidata.iterator().next();
					if (a.getAfter() == b.getAfter() && a.getBefore() == b.getBefore()
							&& StringUtils.equals(a.getTimeString(), b.getTimeString())
							&& a.getPrecision() == b.getPrecision() && a.getTimezone() == b.getTimezone()
							&& TimeValue.CALENDAR_JULIAN.equals(a.getCalendarModel())
							&& TimeValue.CALENDAR_GRIGORIAN.equals(b.getCalendarModel())) {
						return Action.replace;
					}
				}
				Action action = super.getAction(wikipedia, wikidata);
				if (action == Action.append) {
					if (wikidata.isEmpty()) {
						return Action.append;
					} else {
						return Action.report_difference;
					}
				}
				return action;
			}
		});
		parametersToMove.add(new PropertyDescriptor("Дата смерти", DataType.TIME, 570, x -> timeHelper.parse(x)) {
			@Override
			public Action getAction(Collection<DataValue> wikipedia, Collection<DataValue> wikidata) {
				if (wikipedia.size() > 1) {
					return Action.report_difference;
				}
				if (wikipedia.size() == 1 && wikidata.size() == 1) {
					TimeValue a = (TimeValue) wikipedia.iterator().next();
					TimeValue b = (TimeValue) wikidata.iterator().next();
					if (a.getAfter() == b.getAfter() && a.getBefore() == b.getBefore()
							&& StringUtils.equals(a.getTimeString(), b.getTimeString())
							&& a.getPrecision() == b.getPrecision() && a.getTimezone() == b.getTimezone()
							&& TimeValue.CALENDAR_JULIAN.equals(a.getCalendarModel())
							&& TimeValue.CALENDAR_GRIGORIAN.equals(b.getCalendarModel())) {
						return Action.replace;
					}
				}
				Action action = super.getAction(wikipedia, wikidata);
				if (action == Action.append) {
					if (wikidata.isEmpty()) {
						return Action.append;
					} else {
						return Action.report_difference;
					}
				}
				return action;
			}
		});

		parametersToMove.add(new PropertyDescriptor("Гражданство", DataType.WIKIBASE_ITEM, 27,
				new Function<String, List<DataValue>>() {

					Map<String, List<DataValue>> cache = new HashMap<>();
					Map<String, EntityId> queriesCache = new HashMap<>();

					@Override
					public synchronized List<DataValue> apply(String strValue) {
						if (cache.containsKey(strValue)) {
							return cache.get(strValue);
						}

						List<String> countryNames = countriesHelper.normalize(strValue);
						List<DataValue> result = new ArrayList<>();
						for (String countryName : countryNames) {
							if (queriesCache.containsKey(countryName)) {
								final EntityId entityId = queriesCache.get(countryName);
								if (entityId == null) {
									throw new UnsupportedParameterValue(countryName);
								}
								result.add(new WikibaseEntityIdValue(entityId));
								continue;
							}

							Entity countryEntity;
							try {
								countryEntity = wikidataBot.wgGetEntityBySitelink("ruwiki", countryName,
										EntityProperty.info);
								queriesCache.put(countryName, countryEntity.getId());
							} catch (Exception exc) {
								queriesCache.put(countryName, null);
								throw new UnsupportedParameterValue(countryName);
							}

							result.add(new WikibaseEntityIdValue(countryEntity.getId()));
						}
						cache.put(strValue, result);
						return result;
					}
				}) {

			@Override
			public Action getAction(Collection<DataValue> wikipedia, Collection<DataValue> wikidata) {
				Action action = super.getAction(wikipedia, wikidata);
				if (action == Action.append) {
					if (wikipedia.equals(CountriesHelper.VALUES_RUSSIAN_EMPIRE)
							&& wikidata.equals(CountriesHelper.VALUES_RUSSIA)) {
						return Action.replace;
					} else if (wikipedia.equals(CountriesHelper.VALUES_RUSSIAN_EMPIRE_USSR)
							&& wikidata.equals(CountriesHelper.VALUES_RUSSIA)) {
						return Action.replace;
					} else if (wikipedia.equals(CountriesHelper.VALUES_USSR_RUSSIA)
							&& wikidata.equals(CountriesHelper.VALUES_RUSSIA)) {
						return Action.replace;
					} else if (wikipedia.equals(CountriesHelper.VALUES_USSR)
							&& wikidata.equals(CountriesHelper.VALUES_RUSSIA)) {
						return Action.replace;
					} else if (wikidata.isEmpty()) {
						return Action.append;
					} else {
						return Action.report_difference;
					}
				}
				return action;
			}

		});

	}

	private void fillFromWikidata(Entity entity, EntityId property, Set<DataValue> result) {
		for (Statement statement : entity.getClaims(property)) {
			switch (statement.getMainSnak().getSnakType()) {
			case novalue:
				result.add(NOVALUE);
				continue;
			case somevalue:
				continue;
			case value:
				result.add(statement.getMainSnak().getDataValue());
			}
		}
	}

	private void fillFromWikipedia(Template template, PropertyDescriptor descriptor, Set<DataValue> result) {
		for (TemplatePart part : template.getParameters(descriptor.templateParameter)) {
			if (part.getValue() != null) {
				try {
					String value = part.getValue().toWiki(true).trim();
					if (StringUtils.isNotBlank(value)) {
						if (NOVALUES.contains(value)) {
							result.add(NOVALUE);
						} else {
							final List<DataValue> dataValues = descriptor.toWikidata.apply(value);
							result.addAll(dataValues);
						}
					}
				} catch (UnsupportedParameterValue exc) {
					exc.setTemplatePartValue(part.getValue());
					throw exc;
				}
			}
		}
	}

	private void fillToWikidata(PropertyDescriptor descriptor, Set<DataValue> source, JSONObject result) {
		if (!source.isEmpty()) {
			for (DataValue newValue : source) {
				if (NOVALUE.equals(newValue)) {
					ApiStatement statement = ApiStatement.newStatement(descriptor.property, SnakType.novalue);
					ApiEntity.putProperty(result, descriptor.property, statement);
				} else {
					ApiStatement statement = ApiStatement.newStatement(descriptor.property, descriptor.dataType,
							newValue);
					ApiEntity.putProperty(result, descriptor.property, statement);
				}
			}
		}
	}

	private String generateSummary(EntityId templateId, Map<PropertyDescriptor, Set<DataValue>> fromPedia) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("Move [[");
		stringBuilder.append(templateId.toString());
		stringBuilder.append("]] from ruwiki: ");
		for (Map.Entry<PropertyDescriptor, Set<DataValue>> entry : fromPedia.entrySet()) {
			stringBuilder.append("[[Property:" + entry.getKey().property + "]]: ");
			for (DataValue value : entry.getValue()) {
				if (value instanceof StringValue) {
					stringBuilder.append("«" + ((StringValue) value).getValue() + "»; ");
				} else if (value instanceof WikibaseEntityIdValue) {
					stringBuilder.append("[[Q" + ((WikibaseEntityIdValue) value).getNumericId() + "]]; ");
				} else {
					stringBuilder.append(value.toWiki().toWiki(true) + "; ");
				}
			}
		}
		return stringBuilder.toString();
	}

	private void process(EntityId templateId, Revision revision, MoveDataReport report) throws Exception {

		Map<PropertyDescriptor, Set<DataValue>> fromPedia = new LinkedHashMap<>();

		ArticleFragment fragment = ruWikipediaBot.getXmlParser().parse(revision);
		if (fragment.getTemplates(TEMPLATE.toLowerCase()).isEmpty()) {
			return;
		}

		for (PropertyDescriptor descriptor : parametersToMove) {
			try {
				LinkedHashSet<DataValue> fromPediaSet = new LinkedHashSet<>();
				for (Template template : fragment.getAllTemplates().get(TEMPLATE.toLowerCase())) {
					fillFromWikipedia(template, descriptor, fromPediaSet);
				}
				fromPedia.put(descriptor, fromPediaSet);
			} catch (UnsupportedParameterValue exc) {
				report.addLine(revision, descriptor, exc);
				return;
			}
		}

		boolean allSetsAreEmptry = fromPedia.values().stream().allMatch(set -> set.isEmpty());
		if (allSetsAreEmptry) {
			// nothing to move
			return;
		}

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
			entity = wikidataBot.wgGetEntityBySitelink("ruwiki", revision.getPage().getTitle(), EntityProperty.claims);
			if (entity == null) {
				throw new RuntimeException();
			}
			// return;
		}

		Map<PropertyDescriptor, Set<DataValue>> fromData = new HashMap<>();
		for (PropertyDescriptor descriptor : parametersToMove) {
			EntityId property = descriptor.property;
			LinkedHashSet<DataValue> fromDataSet = new LinkedHashSet<>();
			fromData.put(descriptor, fromDataSet);
			fillFromWikidata(entity, property, fromDataSet);
		}

		final List<String> claimIdsToDelete = new ArrayList<>();
		final JSONObject newData = new JSONObject();

		for (PropertyDescriptor descriptor : parametersToMove) {
			Set<DataValue> fromWikipedia = fromPedia.get(descriptor);
			Set<DataValue> fromWikidata = fromData.get(descriptor);

			Action action = descriptor.getAction(fromWikipedia, fromWikidata);
			switch (action) {
			case append:
				fromWikipedia.removeAll(fromWikidata);
				fillToWikidata(descriptor, fromWikipedia, newData);
				break;
			case replace: {
				for (Statement statement : entity.getClaims(descriptor.property)) {
					claimIdsToDelete.add(statement.getId());
				}
				fillToWikidata(descriptor, fromWikipedia, newData);
				break;
			}
			case remove_from_wikipedia:
				fromPedia.remove(descriptor);
				break;
			case report_difference:
				report.addLine(revision, descriptor, fromWikipedia, fromWikidata, entity);
				fromPedia.remove(descriptor);
				continue;
			default:
				throw new UnsupportedOperationException("NYI");
			}

			if (action.removeFromWikipedia) {
				for (Template template : fragment.getAllTemplates().get(TEMPLATE.toLowerCase())) {
					template.removeParameter(descriptor.templateParameter);
				}
			}
		}

		final String summary = generateSummary(templateId, fromPedia);
		if (newData.length() != 0) {
			wikidataBot.wgEditEntity(entity, newData, summary);
		}
		if (!claimIdsToDelete.isEmpty()) {
			wikidataBot.wgRemoveClaims(entity, claimIdsToDelete.toArray(new String[claimIdsToDelete.size()]), summary);
		}

		ruWikipediaBot.writeContent(revision, fragment.toWiki(false), "Move [[Шаблон:" + TEMPLATE
				+ "]] parameters to Wikidata", true);
	}

	@Override
	public void run() {
		EntityId templateId = wikidataBot.wgGetEntityBySitelink("ruwiki", "Шаблон:" + TEMPLATE, EntityProperty.info)
				.getId();
		MoveDataReport report = new MoveDataReport();

		ExecutorService executorService = Executors.newFixedThreadPool(4);
		List<Future<?>> tasks = new ArrayList<>();

		for (Revision revision : ruWikipediaCache.queryByEmbeddedIn("Шаблон:" + TEMPLATE, Namespace.NSS_MAIN)) {
			// for (Revision revision :
			// Collections.singletonList(ruWikipediaCache
			// .queryLatestRevision("Али-заде, Айдын Ариф оглы"))) {
			tasks.add(executorService.submit(new Runnable() {
				@Override
				public void run() {
					try {
						process(templateId, revision, report);
					} catch (Exception exc) {
						log.error(exc.toString(), exc);
						// throw new RuntimeException(e);
					}
				}
			}));
		}

		for (Future<?> task : tasks) {
			try {
				task.get();
			} catch (Exception exc) {
				log.error(exc.toString(), exc);
			}
		}

		report.save(TEMPLATE, ruWikipediaBot);
	}
}
