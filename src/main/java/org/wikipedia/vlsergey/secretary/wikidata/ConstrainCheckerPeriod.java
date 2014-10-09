package org.wikipedia.vlsergey.secretary.wikidata;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.TimeValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ValueType;

@Component
public class ConstrainCheckerPeriod implements Runnable {

	private static final DateTimeFormatter PARSER_DATE = DateTimeFormatter.ofPattern("u-M-d").withZone(ZoneOffset.UTC);

	private static final DateTimeFormatter PARSER_YEAR = DateTimeFormatter.ofPattern("u").withZone(ZoneOffset.UTC);

	@Autowired
	@Qualifier("ruWikipediaBot")
	private MediaWikiBot ruWikipediaBot;

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	private void addFailure(Set<String> failures, Entity toCheckInItem, EntityId toCheckInProperty,
			EntityId toCheckExistenceOf, EntityId timeProperty, TimeValue timeValue) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append(toCheckInProperty.toWikilink(true));
		stringBuilder.append(" (");
		stringBuilder.append(timeProperty.toWikilink(true));
		stringBuilder.append("→");
		stringBuilder.append(timeValue.toWiki(x -> x.toWikilink(false)));
		stringBuilder.append(")");
		failures.add(stringBuilder.toString());
	}

	private void checkIsNotAfter(Entity toCheckInItem, EntityId toCheckInProperty, EntityId toCheckExistenceOf,
			EntityId timeProperty, TemporalAccessor end, Set<String> failures) {
		if (end == null) {
			return;
		}

		for (Statement statement : toCheckInItem.getClaims(timeProperty)) {
			if (statement.hasValue()) {
				final TimeValue timeValue = statement.getMainSnak().getTimeValue();
				TemporalAccessor value = timeValue.getTime();
				if (!value.isSupported(ChronoField.YEAR) || timeValue.getPrecision() < TimeValue.PRECISION_YEAR) {
					continue;
				}
				int year = value.get(ChronoField.YEAR);
				if (year > end.get(ChronoField.YEAR)) {
					addFailure(failures, toCheckInItem, toCheckInProperty, toCheckExistenceOf, timeProperty, timeValue);
					return;
				}
				if (year < end.get(ChronoField.YEAR)) {
					continue;
				}

				if (!end.isSupported(ChronoField.MONTH_OF_YEAR) || !value.isSupported(ChronoField.MONTH_OF_YEAR)
						|| timeValue.getPrecision() < TimeValue.PRECISION_MONTH) {
					continue;
				}
				int month = value.get(ChronoField.MONTH_OF_YEAR);
				if (month > end.get(ChronoField.MONTH_OF_YEAR)) {
					addFailure(failures, toCheckInItem, toCheckInProperty, toCheckExistenceOf, timeProperty, timeValue);
					return;
				}
				if (month < end.get(ChronoField.YEAR)) {
					continue;
				}

				if (!end.isSupported(ChronoField.DAY_OF_MONTH) || !value.isSupported(ChronoField.DAY_OF_MONTH)
						|| timeValue.getPrecision() < TimeValue.PRECISION_DAY) {
					continue;
				}
				int day = value.get(ChronoField.DAY_OF_MONTH);
				if (day > end.get(ChronoField.DAY_OF_MONTH)) {
					addFailure(failures, toCheckInItem, toCheckInProperty, toCheckExistenceOf, timeProperty, timeValue);
					return;
				}
			}
		}
	}

	private void checkIsNotBefore(Entity toCheckInItem, EntityId toCheckInProperty, EntityId toCheckExistenceOf,
			EntityId timeProperty, TemporalAccessor start, Set<String> failures) {
		if (start == null) {
			return;
		}

		for (Statement statement : toCheckInItem.getClaims(timeProperty)) {
			if (statement.hasValue()) {
				final TimeValue timeValue = statement.getMainSnak().getTimeValue();
				TemporalAccessor value = timeValue.getTime();
				if (!value.isSupported(ChronoField.YEAR) || timeValue.getPrecision() < TimeValue.PRECISION_YEAR) {
					continue;
				}
				int year = value.get(ChronoField.YEAR);
				if (year < start.get(ChronoField.YEAR)) {
					addFailure(failures, toCheckInItem, toCheckInProperty, toCheckExistenceOf, timeProperty, timeValue);
					return;
				}
				if (year > start.get(ChronoField.YEAR)) {
					continue;
				}

				if (!start.isSupported(ChronoField.MONTH_OF_YEAR) || !value.isSupported(ChronoField.MONTH_OF_YEAR)
						|| timeValue.getPrecision() < TimeValue.PRECISION_MONTH) {
					continue;
				}
				int month = value.get(ChronoField.MONTH_OF_YEAR);
				if (month < start.get(ChronoField.MONTH_OF_YEAR)) {
					addFailure(failures, toCheckInItem, toCheckInProperty, toCheckExistenceOf, timeProperty, timeValue);
					return;
				}
				if (month > start.get(ChronoField.YEAR)) {
					continue;
				}

				if (!start.isSupported(ChronoField.DAY_OF_MONTH) || !value.isSupported(ChronoField.DAY_OF_MONTH)
						|| timeValue.getPrecision() < TimeValue.PRECISION_DAY) {
					continue;
				}
				int day = value.get(ChronoField.DAY_OF_MONTH);
				if (day < start.get(ChronoField.DAY_OF_MONTH)) {
					addFailure(failures, toCheckInItem, toCheckInProperty, toCheckExistenceOf, timeProperty, timeValue);
					return;
				}
			}
		}
	}

	private TemporalAccessor parse(Content parameterValue) {
		if (parameterValue == null) {
			return null;
		}
		final String value = parameterValue.toWiki(true);
		if (StringUtils.isBlank(value)) {
			return null;
		}
		if (value.matches("^[0-9]+$")) {
			return PARSER_YEAR.parse(value);
		}
		return PARSER_DATE.parse(value);
	}

	private boolean presentIn(Entity entity, EntityId property, EntityId itemToCheck) {
		for (Statement statement : entity.getClaims(property)) {
			if (statement.isWikibaseEntityIdValue(itemToCheck)) {
				return true;
			}
		}
		return false;
	}

	private boolean presentInR(Entity entity, EntityId property, EntityId[] qualifiersIds, EntityId itemToCheck) {
		for (Statement statement : entity.getClaims(property)) {
			if (statement.isWikibaseEntityIdValue(itemToCheck)) {
				return true;
			}
			for (EntityId qualifierId : qualifiersIds) {
				for (Snak qualifier : statement.getQualifiers(qualifierId)) {
					if (qualifier.isWikibaseEntityIdValue(itemToCheck)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	@Override
	public void run() {
		final TitleResolver titleResolver = new TitleResolver(wikidataCache);

		// for (Revision itemTalkRevision :
		// Collections.singletonList(wikidataCache.queryLatestRevision("Talk:Q184")))
		// {
		for (Revision itemTalkRevision : wikidataCache.queryByEmbeddedIn("Template:Constraint:Period", Namespace.TALK)) {
			try {
				EntityId itemToCheckId = EntityId.parse(itemTalkRevision.getPage().getTitle()
						.substring("Talk:".length()));
				Entity itemToCheckEntity = new Entity(wikidataCache.queryLatestRevision(itemToCheckId.getPageTitle()));
				ArticleFragment fragment = wikidataCache.getMediaWikiBot().getXmlParser().parse(itemTalkRevision);

				TemporalAccessor start = null;
				TemporalAccessor end = null;

				for (Template template : fragment.getTemplates("Constraint:Period")) {
					start = parse(template.getParameterValue("start"));
					end = parse(template.getParameterValue("end"));
				}

				System.err.println("Need to check that usage of " + itemToCheckId + " is in period " + start + " — "
						+ end);

				Map<EntityId, Entity> cache = new HashMap<EntityId, Entity>();
				SortedMap<EntityId, Set<String>> failures = new TreeMap<>();

				int checkedItems = 0;
				int problemItems = 0;
				final Long itemPageId = wikidataCache.queryLatestRevision(itemToCheckId.getPageTitle()).getPage()
						.getId();
				for (Revision toCheck : wikidataCache.queryByBacklinks(itemPageId, Namespace.NSS_MAIN)) {
					// for (Revision toCheck :
					// Collections.singletonList(wikidataCache.queryLatestRevision("Q15640147")))
					// {
					final Entity entity = new Entity(new JSONObject(toCheck.getContent()));
					final EntityId entityId = entity.getId();
					Set<String> itemFailures = new TreeSet<>();

					// nationality
					if (presentIn(entity, Properties.NATIONALITY, itemToCheckId)) {
						checkIsNotAfter(entity, Properties.NATIONALITY, itemToCheckId, Properties.DATE_OF_BIRTH, end,
								itemFailures);
						checkIsNotBefore(entity, Properties.NATIONALITY, itemToCheckId, Properties.DATE_OF_DEATH,
								start, itemFailures);
					}
					if (presentInR(entity, Properties.PLACE_OF_BIRTH, new EntityId[] { Properties.ADMINISTRATIVE_UNIT,
							Properties.COUNTRY }, itemToCheckId)) {
						checkIsNotBefore(entity, Properties.PLACE_OF_BIRTH, itemToCheckId, Properties.DATE_OF_BIRTH,
								start, itemFailures);
						checkIsNotAfter(entity, Properties.PLACE_OF_BIRTH, itemToCheckId, Properties.DATE_OF_BIRTH,
								end, itemFailures);
					}
					if (presentInR(entity, Properties.PLACE_OF_DEATH, new EntityId[] { Properties.ADMINISTRATIVE_UNIT,
							Properties.COUNTRY }, itemToCheckId)) {
						checkIsNotBefore(entity, Properties.PLACE_OF_DEATH, itemToCheckId, Properties.DATE_OF_DEATH,
								start, itemFailures);
						checkIsNotAfter(entity, Properties.PLACE_OF_DEATH, itemToCheckId, Properties.DATE_OF_DEATH,
								end, itemFailures);
					}

					checkedItems++;
					if (!itemFailures.isEmpty()) {
						problemItems++;
						cache.put(entityId, entity);
						failures.put(entityId, new TreeSet<>(itemFailures));
					}
				}

				{
					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append("{{constraint violations report description:period|itemId=" + itemToCheckId
							+ "}}\n\n");

					stringBuilder.append("\n\n");
					stringBuilder.append("Checked items: " + checkedItems + "\n\n");
					stringBuilder.append("Problem items: " + problemItems + "\n\n");
					for (EntityId key : failures.keySet()) {
						stringBuilder.append("* [[");
						stringBuilder.append(key);
						stringBuilder.append("]] —");
						for (String failure : failures.get(key)) {
							stringBuilder.append(" ");
							stringBuilder.append(failure);
							stringBuilder.append(";");
						}
						stringBuilder.append("\n");
					}

					wikidataCache.getMediaWikiBot().writeContent(
							"Wikidata:Database reports/Constraint violations/" + itemToCheckId + "/Period", null,
							stringBuilder.toString(), null,
							"Update constrains report " + checkedItems + " / " + problemItems, true, false);
				}
				{

					// prebuild cache of titles
					{
						final EntityId[] propertiesToCacheTitles = new EntityId[] { Properties.NATIONALITY,
								Properties.DATE_OF_BIRTH, Properties.PLACE_OF_BIRTH, Properties.DATE_OF_DEATH,
								Properties.PLACE_OF_DEATH, Properties.PLACE_OF_BURIAL };

						Set<EntityId> ids = new TreeSet<>();
						ids.addAll(Arrays.asList(propertiesToCacheTitles));
						for (EntityId entityId : failures.keySet()) {
							try {
								ids.add(entityId);
								Entity entity = cache.get(entityId);
								for (EntityId propertyId : propertiesToCacheTitles) {
									for (Statement statement : entity.getClaims(propertyId)) {
										if (statement.hasValue()
												&& statement.getValueType() == ValueType.WIKIBASE_ENTITYID) {
											ids.add(statement.getMainSnak().getWikibaseEntityIdValue().getEntityId());
										}
										for (Snak snak : statement.getQualifiers()) {
											if (snak.hasValue() && snak.getValueType() == ValueType.WIKIBASE_ENTITYID) {
												ids.add(snak.getWikibaseEntityIdValue().getEntityId());
											}
										}
									}
								}
							} catch (Exception exc) {
								exc.printStackTrace();
							}
						}

						for (Revision revision : wikidataCache.queryLatestByPageTitles(
								ids.stream().map(x -> x.getPageTitle()).collect(Collectors.toSet()), false)) {
							try {
								titleResolver.update(new Entity(revision));
							} catch (Exception exc) {
								exc.printStackTrace();
							}
						}
					}

					StringBuilder stringBuilder = new StringBuilder();
					stringBuilder.append("{{Constraint violations report:Period|itemId=" + itemToCheckId
							+ "|itemLabel=" + StringUtils.trimToEmpty(itemToCheckEntity.getLabelValue("ru"))
							+ "|itemTitle=" + StringUtils.trimToEmpty(itemToCheckEntity.getSiteLinkTitle("ruwiki"))
							+ "}}\n\n");

					stringBuilder.append("\n\n");
					stringBuilder.append("Проверено элементов: " + checkedItems + "\n\n");
					stringBuilder.append("Проблемных элементов: " + problemItems + "\n\n");
					stringBuilder.append("Ниже перечислены элементы, присутствующие в текущем проекте:\n\n");
					stringBuilder.append("{| class=\"wikitable sortable\" |\n");
					stringBuilder.append("! \n");
					stringBuilder.append("! \n");
					stringBuilder.append("! [[:d:" + Properties.NATIONALITY.getPageTitle()
							+ "|гражданство / подданство]]\n");
					stringBuilder.append("! [[:d:" + Properties.DATE_OF_BIRTH.getPageTitle() + "|дата рождения]]\n");
					stringBuilder.append("! [[:d:" + Properties.PLACE_OF_BIRTH.getPageTitle() + "|место рождения]]\n");
					stringBuilder.append("! [[:d:" + Properties.DATE_OF_DEATH.getPageTitle() + "|дата смерти]]\n");
					stringBuilder.append("! [[:d:" + Properties.PLACE_OF_DEATH.getPageTitle() + "|место смерти]]\n");
					stringBuilder.append("! [[:d:" + Properties.PLACE_OF_BURIAL.getPageTitle()
							+ "|место погребения]]\n");
					stringBuilder.append("|-\n");
					for (EntityId entityId : failures.keySet()) {
						Entity entity = cache.get(entityId);
						if (entity.hasSitelink("ruwiki")) {
							stringBuilder.append("| " + entityId.toWikilink(true) + "\n");
							stringBuilder.append("| [[" + entity.getSiteLink("ruwiki").getTitle() + "]]\n");

							stringBuilder.append("| " + toString(titleResolver, entity, Properties.NATIONALITY) + "\n");
							stringBuilder.append("| " + toString(titleResolver, entity, Properties.DATE_OF_BIRTH)
									+ "\n");
							stringBuilder.append("| " + toString(titleResolver, entity, Properties.PLACE_OF_BIRTH)
									+ "\n");
							stringBuilder.append("| " + toString(titleResolver, entity, Properties.DATE_OF_DEATH)
									+ "\n");
							stringBuilder.append("| " + toString(titleResolver, entity, Properties.PLACE_OF_DEATH)
									+ "\n");
							stringBuilder.append("| " + toString(titleResolver, entity, Properties.PLACE_OF_BURIAL)
									+ "\n");
							stringBuilder.append("|-\n");
						}
					}
					stringBuilder.append("|}\n");

					ruWikipediaBot.writeContent("User:" + ruWikipediaBot.getLogin() + "/Constraint violations/"
							+ itemToCheckId + "/Period", null, stringBuilder.toString(), null,
							"Update constrains report " + checkedItems + " / " + problemItems, true, false);
				}

			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}
	}

	private String toString(TitleResolver titleResolver, Entity entity, EntityId property) {
		final List<Statement> claims = entity.getClaims(property);
		if (claims == null || claims.isEmpty()) {
			return StringUtils.EMPTY;
		}
		if (claims.size() == 1) {
			final ValueWithQualifiers value = ValueWithQualifiers.fromStatements(claims).get(0);
			String result = value.toString(titleResolver, 0);
			if (value.getValue().hasValue() && value.getValue().getValueType() == ValueType.TIME) {
				return " data-sort-value=\"" + value.getValue().getTimeValue().getTimeString() + "\" | " + result;
			}
			return result;
		}
		return "\n* "
				+ StringUtils.join(
						ValueWithQualifiers.fromStatements(claims).stream().map(x -> x.toString(titleResolver, 1))
								.iterator(), "\n*");
	}
}
