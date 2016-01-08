package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;

@Component
public class PlacesHelper extends AbstractHelper implements DictionaryUpdateListener {

	private static final Set<String> EMPTY = new HashSet<>(Arrays.asList("{{Место рождения}}", "{{место рождения}}",
			"{{МестоРождения}}", "{{МР}}", "{{Место смерти}}", "{{место смерти}}", "{{МестоСмерти}}", "{{МС}}"));

	private static final EntityId ENTITY_city_state = EntityId.item(133442);
	private static final EntityId ENTITY_confederation = EntityId.item(170156);
	private static final EntityId ENTITY_country = EntityId.item(6256);
	private static final EntityId ENTITY_empyre = EntityId.item(48349);
	private static final EntityId ENTITY_former_country = EntityId.item(16905642);
	private static final EntityId ENTITY_sovereign_state = EntityId.item(3624078);
	private static final EntityId ENTITY_state = EntityId.item(7275);
	private static final EntityId ENTITY_un_member = EntityId.item(160016);
	private static final EntityId ENTITY_usa_unincorporated_territory = EntityId.item(783733);

	private static final Log log = LogFactory.getLog(PlacesHelper.class);

	private static Map<String, String> REPLACED_LABELS = new HashMap<>();

	private static final EntityId[] TOCHECK = { ENTITY_city_state, ENTITY_confederation, ENTITY_country,
			ENTITY_former_country, ENTITY_empyre, ENTITY_sovereign_state, ENTITY_state, ENTITY_un_member,
			ENTITY_usa_unincorporated_territory };

	static {
		REPLACED_LABELS.put("Азербайджанская ССР", "Азербайджанская Советская Социалистическая Республика");
		REPLACED_LABELS.put("АзССР", "Азербайджанская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Армянская ССР", "Армянская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Башкирская АССР", "Башкирская Автономная Советская Социалистическая Республика");
		REPLACED_LABELS.put("БССР", "Белорусская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Белорусская ССР", "Белорусская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Беларусь", "Белоруссия");
		REPLACED_LABELS.put("Грузинская ССР", "Грузинская Советская Социалистическая Республика");
		REPLACED_LABELS.put("ГДР", "Германская Демократическая Республика");
		REPLACED_LABELS.put("Дагестанская АССР", "Дагестанская Автономная Советская Социалистическая Республика");
		REPLACED_LABELS.put("Казакская АССР", "Казакская Автономная Социалистическая Советская Республика");
		REPLACED_LABELS.put("Казахская ССР", "Казахская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Латвийская ССР", "Латвийская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Литовская ССР", "Литовская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Молдавская ССР", "Молдавская Советская Социалистическая Республика");
		REPLACED_LABELS.put("МССР", "Молдавская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Нью-Йорк (штат)", "Нью-Йорк");
		REPLACED_LABELS.put("Российская Федерация", "Россия");
		// REPLACED_LABELS.put("РСФСР",
		// "Российская Советская Федеративная Социалистическая Республика");
		// REPLACED_LABELS.put("СССР",
		// "Союз Советских Социалистических Республик");
		REPLACED_LABELS.put("США", "Соединённые Штаты Америки");
		REPLACED_LABELS.put("Таджикская ССР", "Таджикская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Татарская АССР", "Татарская Автономная Советская Социалистическая Республика");
		REPLACED_LABELS.put("Туркестанская АССР", "Туркестанская Автономная Социалистическая Советская Республика");
		REPLACED_LABELS.put("Туркменская ССР", "Туркменская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Украинская ССР", "Украинская Советская Социалистическая Республика");
		REPLACED_LABELS.put("УССР", "Украинская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Узбекская ССР", "Узбекская Советская Социалистическая Республика");
		REPLACED_LABELS.put("Эстонская ССР", "Эстонская Советская Социалистическая Республика");
	}

	private static String getArticleFromArticleAndLabel(String str) {
		return str.contains("|") ? StringUtils.substringBefore(str, "|") : str;
	}

	private static String getLabelFromArticleAndLabel(String str) {
		String label = str.contains("|") ? StringUtils.substringAfter(str, "|") : str;
		if (REPLACED_LABELS.containsKey(label)) {
			return REPLACED_LABELS.get(label);
		}
		return label;
	}

	private Set<EntityId> countries;

	private Map<EntityId, Set<EntityId>> hasAutoCategories = new HashMap<>(2);

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	void assertFound(String title, Entity entity) {
		if (entity == null) {
			throw new NoWikidataElementException(title);
		}
	}

	void assertHasAutocategory(boolean required, EntityId property, String place, Entity placeEntity) {
		if (required && !hasAutoCategories.get(property).contains(placeEntity.getId())) {
			throw new AutocategorizationRequiredException(place, placeEntity.getId());
		}
	}

	void assertSameLabel(String expectedLabel, Entity entity) {
		if (!entity.hasLabel("ru") || !StringUtils.equalsIgnoreCase(expectedLabel, entity.getLabel("ru").getValue())) {
			throw new NotSameLabelException(entity.hasLabel("ru") ? entity.getLabel("ru").getValue() : "(missing)",
					expectedLabel);
		}
	}

	private Set<EntityId> getWithProperty(EntityId propertyId) {
		Set<EntityId> entityIds = new HashSet<>();
		for (Revision rev : wikidataCache.queryByBacklinks(wikidataCache.queryLatestRevision(propertyId.getPageTitle())
				.getPage().getId(), Namespace.NSS_MAIN)) {
			Entity entity = new Entity(new JSONObject(rev.getContent()));
			if (entity.hasClaims(propertyId)) {
				entityIds.add(entity.getId());
			}
		}
		return entityIds;
	}

	@PostConstruct
	public void init() {
		synchronized (this) {
			this.hasAutoCategories.put(Properties.PLACE_OF_BIRTH,
					getWithProperty(Properties.CATEGORY_FOR_PEOPLE_BORN_HERE));
			this.hasAutoCategories.put(Properties.PLACE_OF_DEATH,
					getWithProperty(Properties.CATEGORY_FOR_PEOPLE_DIED_HERE));
		}

		loadCountries();
	}

	private void loadCountries() {
		Set<EntityId> countries = new HashSet<>();
		for (EntityId toCheck : TOCHECK) {
			final Revision stateTypeItemRev = wikidataCache.queryLatestRevision(toCheck.toString().toUpperCase());
			if (stateTypeItemRev == null) {
				log.error("Unknown entity ID: " + toCheck);
			}
			final Page stateTypeItemPage = stateTypeItemRev.getPage();

			for (Revision revision : wikidataCache.queryByBacklinks(stateTypeItemPage.getId(), Namespace.MAIN)) {
				Entity entity = new Entity(new JSONObject(revision.getContent()));
				countries.add(entity.getId());
			}
		}
		setCountries(countries);
	}

	@Override
	public synchronized void onUpdate(EntityId property, SortedMap<EntityId, List<Statement>> result) {
		if (Properties.CATEGORY_FOR_PEOPLE_BORN_HERE.equals(property)) {
			this.hasAutoCategories.put(Properties.PLACE_OF_BIRTH, new HashSet<>(result.keySet()));
		}
		if (Properties.CATEGORY_FOR_PEOPLE_DIED_HERE.equals(property)) {
			this.hasAutoCategories.put(Properties.PLACE_OF_DEATH, new HashSet<>(result.keySet()));
		}
	}

	public List<ValueWithQualifiers> parse(final EntityByLinkResolver entityByLinkResolver, EntityId property,
			String original) {

		String strValue = StringUtils.trimToEmpty(original);

		strValue = strValue.replaceAll("<br />", " ");
		strValue = strValue.replaceAll("  ", " ");
		strValue = strValue.replaceAll("\\|\\}\\}", "}}");
		strValue = strValue.replaceAll("\\|\\}\\}", "}}");
		strValue = strValue.replaceAll("\\|\\}\\}", "}}");

		if (strValue.startsWith("г. ")) {
			strValue = strValue.substring("г. ".length());
		}
		if (strValue.startsWith("город ")) {
			strValue = strValue.substring("город ".length());
		}

		boolean autocategoriesRequired = false;

		if (UNKNOWN.contains(strValue.toLowerCase())) {
			return ValueWithQualifiers.fromSnak(Snak.newSnak(property, SnakType.somevalue));
		}
		if (EMPTY.contains(strValue)) {
			return Collections.emptyList();
		}

		if (strValue.matches(".*\\{\\{(М[РС]|[Мм]есто\\s?([Рр]ождения|[Сс]мерти))\\|([^\\[\\]\\{\\}\\|]*)\\}\\}.*")) {
			strValue = strValue.replaceFirst(
					"\\{\\{(М[РС]|[Мм]есто\\s?([Рр]ождения|[Сс]мерти))\\|([^\\[\\]\\{\\}\\|]*)\\}\\}", "[[$3]]");
			autocategoriesRequired = true;
		}
		if (strValue
				.matches(".*\\{\\{(М[РС]|[Мм]есто\\s?([Рр]ождения|[Сс]мерти))\\|([^\\[\\]\\{\\}\\|]*)\\|([^\\[\\]\\{\\}\\|]+)\\}\\}.*")) {
			strValue = strValue
					.replaceFirst(
							"\\{\\{(М[РС]|[Мм]есто\\s?([Рр]ождения|[Сс]мерти))\\|([^\\[\\]\\{\\}\\|]*)\\|([^\\[\\]\\{\\}\\|]+)\\}\\}",
							"[[$3]]");
			autocategoriesRequired = true;
		}
		if (strValue
				.matches(".*\\{\\{(М[РС]|[Мм]есто\\s?([Рр]ождения|[Сс]мерти))\\|([^\\[\\]\\{\\}\\|]*)\\|([^\\[\\]\\{\\}\\|]+)\\|([^\\[\\]\\{\\}\\|]+)\\}\\}.*")) {
			strValue = strValue
					.replaceFirst(
							"\\{\\{(М[РС]|[Мм]есто\\s?([Рр]ождения|[Сс]мерти))\\|([^\\[\\]\\{\\}\\|]*)\\|([^\\[\\]\\{\\}\\|]+)\\|([^\\[\\]\\{\\}\\|]+)\\}\\}",
							"[[$5|$3]]");
			autocategoriesRequired = true;
		}

		strValue = strValue.replaceAll("\\,\\s*Германия$", ", [[Германия]]");
		strValue = strValue.replaceAll("\\,\\s*СССР$", ", [[СССР]]");
		strValue = strValue.replaceAll("\\,\\s*США$", ", [[США]]");
		strValue = strValue.replaceAll("\\,\\s*Россия$", ", [[Россия]]");
		strValue = strValue.replaceAll("\\,\\s*Франция$", ", [[Франция]]");

		if (strValue.matches("^\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\]\\s*$")) {
			Matcher matcher = Pattern.compile("^\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\]\\s*$").matcher(strValue);
			matcher.matches();
			String placeArticleAndLabel = matcher.group(1);

			String placeArticle = getArticleFromArticleAndLabel(placeArticleAndLabel);
			String placeLabel = getLabelFromArticleAndLabel(placeArticleAndLabel);

			Entity placeEntity = entityByLinkResolver.apply(placeArticle);
			assertFound(placeArticle, placeEntity);
			assertSameLabel(placeLabel, placeEntity);
			assertHasAutocategory(autocategoriesRequired, property, placeArticle, placeEntity);

			return Collections.singletonList(new ValueWithQualifiers(Snak.newSnak(property, placeEntity.getId()),
					Collections.emptyList()));
		}

		if (strValue.matches("^\\[\\[([^\\[\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\]$")) {
			Matcher matcher = Pattern.compile("^\\[\\[([^\\[\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\]$")
					.matcher(strValue);
			matcher.matches();
			String placeArticleAndLabel = matcher.group(1);
			String possibleCountryArticleAndLabel = matcher.group(2);

			String placeArticle = getArticleFromArticleAndLabel(placeArticleAndLabel);
			String placeLabel = getLabelFromArticleAndLabel(placeArticleAndLabel);
			String possibleCountryArticle = getArticleFromArticleAndLabel(possibleCountryArticleAndLabel);
			String possibleCountryLabel = getLabelFromArticleAndLabel(possibleCountryArticleAndLabel);

			Entity placeEntity = entityByLinkResolver.apply(placeArticle);
			assertFound(placeArticle, placeEntity);
			assertSameLabel(placeLabel, placeEntity);
			assertHasAutocategory(autocategoriesRequired, property, placeArticle, placeEntity);

			Entity countryEntity = entityByLinkResolver.apply(possibleCountryArticle);
			assertFound(possibleCountryArticle, countryEntity);
			assertSameLabel(possibleCountryLabel, countryEntity);

			if (!countries.contains(countryEntity.getId())) {
				throw new NotInCountriesListException(possibleCountryArticle);
			}

			return Collections.singletonList(new ValueWithQualifiers(Snak.newSnak(property, placeEntity.getId()),
					Collections.singletonList(Snak.newSnak(Properties.COUNTRY, countryEntity.getId()))));
		}

		if (strValue
				.matches("^\\[\\[([^\\[\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\]$")) {

			Matcher matcher = Pattern
					.compile(
							"^\\[\\[([^\\[\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\]$")
					.matcher(strValue);
			matcher.matches();
			String placeArticleAndLabel = matcher.group(1);
			String admUnit1ArticleAndLabel = matcher.group(2);
			String possibleCountryArticleAndLabel = matcher.group(3);

			String placeArticle = getArticleFromArticleAndLabel(placeArticleAndLabel);
			String placeLabel = getLabelFromArticleAndLabel(placeArticleAndLabel);
			String admUnit1Article = getArticleFromArticleAndLabel(admUnit1ArticleAndLabel);
			String admUnit1Label = getLabelFromArticleAndLabel(admUnit1ArticleAndLabel);
			String possibleCountryArticle = getArticleFromArticleAndLabel(possibleCountryArticleAndLabel);
			String possibleCountryLabel = getLabelFromArticleAndLabel(possibleCountryArticleAndLabel);

			Entity placeEntity = entityByLinkResolver.apply(placeArticle);
			assertFound(placeArticle, placeEntity);
			assertSameLabel(placeLabel, placeEntity);
			assertHasAutocategory(autocategoriesRequired, property, placeArticle, placeEntity);

			Entity admUnitEntity1 = entityByLinkResolver.apply(admUnit1Article);
			assertFound(admUnit1Article, admUnitEntity1);
			assertSameLabel(admUnit1Label, admUnitEntity1);

			Entity countryEntity = entityByLinkResolver.apply(possibleCountryArticle);
			assertFound(possibleCountryArticle, countryEntity);
			assertSameLabel(possibleCountryLabel, countryEntity);

			if (!countries.contains(countryEntity.getId())) {
				throw new NotInCountriesListException(possibleCountryArticle);
			}

			return Collections.singletonList(new ValueWithQualifiers(Snak.newSnak(property, placeEntity.getId()),
					Arrays.asList(Snak.newSnak(Properties.ADMINISTRATIVE_UNIT, admUnitEntity1.getId()),
							Snak.newSnak(Properties.COUNTRY, countryEntity.getId()))));
		}

		if (strValue
				.matches("^\\[\\[([^\\[\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\|\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\]$")) {

			Matcher matcher = Pattern
					.compile(
							"^\\[\\[([^\\[\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\|\\]\\{\\}]+)\\]\\],\\s*\\[\\[([^\\[\\]\\{\\}]+)\\]\\]$")
					.matcher(strValue);
			matcher.matches();
			String placeArticleAndLabel = matcher.group(1);
			String admUnit1ArticleAndLabel = matcher.group(2);
			String admUnit2ArticleAndLabel = matcher.group(3);
			String possibleCountryArticleAndLabel = matcher.group(4);

			String placeArticle = getArticleFromArticleAndLabel(placeArticleAndLabel);
			String placeLabel = getLabelFromArticleAndLabel(placeArticleAndLabel);
			String admUnit1Article = getArticleFromArticleAndLabel(admUnit1ArticleAndLabel);
			String admUnit1Label = getLabelFromArticleAndLabel(admUnit1ArticleAndLabel);
			String admUnit2Article = getArticleFromArticleAndLabel(admUnit2ArticleAndLabel);
			String admUnit2Label = getLabelFromArticleAndLabel(admUnit2ArticleAndLabel);
			String possibleCountryArticle = getArticleFromArticleAndLabel(possibleCountryArticleAndLabel);
			String possibleCountryLabel = getLabelFromArticleAndLabel(possibleCountryArticleAndLabel);

			Entity placeEntity = entityByLinkResolver.apply(placeArticle);
			assertFound(placeArticle, placeEntity);
			assertSameLabel(placeLabel, placeEntity);
			assertHasAutocategory(autocategoriesRequired, property, placeArticle, placeEntity);

			Entity admUnitEntity1 = entityByLinkResolver.apply(admUnit1Article);
			assertFound(admUnit1Article, admUnitEntity1);
			assertSameLabel(admUnit1Label, admUnitEntity1);

			Entity admUnitEntity2 = entityByLinkResolver.apply(admUnit2Article);
			assertFound(admUnit2Article, admUnitEntity2);
			assertSameLabel(admUnit2Label, admUnitEntity2);

			Entity countryEntity = entityByLinkResolver.apply(possibleCountryArticle);
			assertFound(possibleCountryArticle, countryEntity);
			assertSameLabel(possibleCountryLabel, countryEntity);

			if (!countries.contains(countryEntity.getId())) {
				throw new NotInCountriesListException(possibleCountryArticle);
			}

			return Collections.singletonList(new ValueWithQualifiers(Snak.newSnak(property, placeEntity.getId()),
					Arrays.asList(Snak.newSnak(Properties.ADMINISTRATIVE_UNIT, admUnitEntity1.getId()),
							Snak.newSnak(Properties.ADMINISTRATIVE_UNIT, admUnitEntity2.getId()),
							Snak.newSnak(Properties.COUNTRY, countryEntity.getId()))));
		}

		synchronized (this) {
			if (strValue.matches("^[A-Za-zА-ЯЁа-яё\\-\\s\\']*$")) {

				Entity entity = entityByLinkResolver.apply(strValue);
				if (entity == null) {
					throw new CantParseValueException(strValue);
				}

				if (entity.hasLabel("ru") && StringUtils.equalsIgnoreCase(strValue, entity.getLabel("ru").getValue())) {
					assertHasAutocategory(autocategoriesRequired, property, strValue, entity);
					return ValueWithQualifiers.fromSnak(Snak.newSnak(property, entity.getId()));
				}
			}
		}

		throw new CantParseValueException(strValue);
	}

	void setCountries(Set<EntityId> countries) {
		this.countries = countries;
	}
}
