package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiSnak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;

@Component
public class PlacesHelper extends AbstractHelper implements DictionaryUpdateListener {

	private Map<String, EntityId[]> DICTIONARY = new HashMap<>();

	private Map<EntityId, Set<EntityId>> hasAutoCategories = new HashMap<>(2);

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	{
		DICTIONARY.put("[[Москва]], [[Российская империя]]",
				new EntityId[] { Places.Москва, Places.Российская_империя });

		DICTIONARY.put("Москва, [[СССР]]", new EntityId[] { Places.Москва, Places.РСФСР, Places.СССР });
		DICTIONARY.put("[[Москва]], [[СССР]]", new EntityId[] { Places.Москва, Places.РСФСР, Places.СССР });
		DICTIONARY.put("[[Москва]], [[РСФСР]], [[СССР]]", new EntityId[] { Places.Москва, Places.РСФСР, Places.СССР });

		DICTIONARY.put("[[Москва]], [[Россия]]", new EntityId[] { Places.Москва, Places.Россия });
		DICTIONARY.put("{{МестоСмерти|Москва|в Москве}}, Россия", new EntityId[] { Places.Москва, Places.Россия });

		DICTIONARY.put("[[Санкт-Петербург]], [[Российская империя]]", new EntityId[] { Places.Санкт_Петербург,
				Places.Российская_империя });
		DICTIONARY.put("[[Санкт-Петербург]], [[Российская Федерация]]", new EntityId[] { Places.Санкт_Петербург,
				Places.Россия });
	}

	private Set<EntityId> getWithProperty(EntityId propertyId) {
		Set<EntityId> entityIds = new HashSet<>();
		for (Revision rev : wikidataCache.queryByBacklinks(wikidataCache.queryLatestRevision(propertyId.getPageTitle())
				.getPage().getId(), Namespace.NSS_MAIN)) {
			Entity entity = new ApiEntity(new JSONObject(rev.getContent()));
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
		boolean autocategoriesRequired = false;

		if (strValue.matches("^\\{\\{Место\\s?([Рр]ождения|[Сс]мерти)\\|([А-Яа-я\\s\\-]+)\\}\\}.*")) {
			strValue = strValue.replaceFirst("^\\{\\{Место\\s?([Рр]ождения|[Сс]мерти)\\|([А-Яа-я\\s\\-]+)\\}\\}",
					"[[$2]]");
			autocategoriesRequired = true;
		}
		if (strValue.matches("^\\{\\{(МР|МС)\\|([А-Яа-я\\s\\-]+)\\}\\}.*")) {
			strValue = strValue.replaceFirst("^\\{\\{(МР|МС)\\|([А-Яа-я\\s\\-]+)\\}\\}", "[[$2]]");
			autocategoriesRequired = true;
		}

		if (DICTIONARY.containsKey(strValue)) {
			EntityId[] places = DICTIONARY.get(strValue);
			ApiSnak apiSnak = ApiSnak.newSnak(property, places[0]);

			if (autocategoriesRequired && !hasAutoCategories.get(property).contains(places[0])) {
				throw new AutocategorizationRequiredException(strValue);
			}

			// last always country
			List<ApiSnak> qualifiers = new ArrayList<ApiSnak>();
			for (int i = 1; i < places.length; i++) {
				if (i != places.length - 1) {
					// unit
					qualifiers.add(ApiSnak.newSnak(Properties.ADMINISTRATIVE_UNIT, places[i]));
				} else {
					// country
					qualifiers.add(ApiSnak.newSnak(Properties.COUNTRY, places[i]));
				}
			}

			ValueWithQualifiers valueWithQualifiers = new ValueWithQualifiers(apiSnak, qualifiers);
			return Collections.singletonList(valueWithQualifiers);
		}

		if (strValue.matches("^\\[\\[[^\\[\\]\\{\\}]+\\]\\]*$")) {
			strValue = strValue.replaceFirst("^\\[\\[([^\\[\\]\\{\\}]+)\\]\\]*$", "$1");
		}

		synchronized (this) {
			if (strValue.matches("^[A-Za-zА-Яа-я\\-\\s]*$")) {

				Entity entity = entityByLinkResolver.apply(strValue);
				if (entity == null) {
					throw new CantParseValueException(strValue);
				}

				if (entity.hasLabel("ru") && StringUtils.equalsIgnoreCase(strValue, entity.getLabel("ru").getValue())) {
					if (autocategoriesRequired && !hasAutoCategories.get(property).contains(entity.getId())) {
						throw new AutocategorizationRequiredException(strValue);
					}
					return ValueWithQualifiers.fromSnak(ApiSnak.newSnak(property, entity.getId()));
				}
			}
		}

		throw new CantParseValueException(strValue);
	}
}
