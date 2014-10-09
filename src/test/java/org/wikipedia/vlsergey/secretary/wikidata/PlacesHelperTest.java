package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Label;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;

public class PlacesHelperTest {

	@Test
	public void getAction() {

		final PlacesHelper placesHelper = mockPlacesHelper();
		final EntityByLinkResolver entityByLinkResolver = mockEntityByLinkResolver();

		final List<ValueWithQualifiers> withCountry = placesHelper.parse(entityByLinkResolver,
				Properties.PLACE_OF_BIRTH, "{{МестоРождения|Санкт-Петербург}},<br /> [[Российская империя]]");
		final List<ValueWithQualifiers> withoutCountry = placesHelper.parse(entityByLinkResolver,
				Properties.PLACE_OF_BIRTH, "{{МестоРождения|Санкт-Петербург}}");

		Assert.assertEquals(ReconsiliationAction.report_difference, placesHelper.getAction(withCountry, withoutCountry));
	}

	private EntityByLinkResolver mockEntityByLinkResolver() {
		return new EntityByLinkResolver(new WikiCache(), null) {
			@Override
			public Entity apply(String strValue) {
				if (strValue.equals("Калифорния")) {
					return fakeEntity(Places.Калифорния, strValue);
				}
				if (strValue.equals("Кишинёв")) {
					return fakeEntity(Places.Кишинёв, strValue);
				}
				if (strValue.equals("Лос-Анджелес")) {
					return fakeEntity(Places.Лос_Анджелес, strValue);
				}
				if (strValue.equals("Молдавская Советская Социалистическая Республика") || strValue.equals("МССР")) {
					return fakeEntity(Places.МССР, "Молдавская Советская Социалистическая Республика");
				}
				if (strValue.equals("Москва")) {
					return fakeEntity(Places.Москва, strValue);
				}
				if (strValue.equals("Нью-Йорк")) {
					return fakeEntity(Places.Нью_Йорк_город, strValue);
				}
				if (strValue.equals("Нью-Йорк (штат)")) {
					return fakeEntity(Places.Нью_Йорк_штат, "Нью-Йорк");
				}
				if (strValue.equals("Одесса")) {
					return fakeEntity(Places.Одесса, strValue);
				}
				if (strValue.equals("Российская империя")) {
					return fakeEntity(Places.Российская_империя, strValue);
				}
				if (strValue.equals("Российская Советская Федеративная Социалистическая Республика")
						|| strValue.equals("РСФСР")) {
					return fakeEntity(Places.РСФСР, "Российская Советская Федеративная Социалистическая Республика");
				}
				if (strValue.equals("Ростов-на-Дону")) {
					return fakeEntity(Places.Ростов_на_Дону, "Ростов-на-Дону");
				}
				if (strValue.equals("Санкт-Петербург")) {
					return fakeEntity(Places.Санкт_Петербург, strValue);
				}
				if (strValue.equals("Соединённые Штаты Америки") || strValue.equals("США")) {
					return fakeEntity(Places.США, "Соединённые Штаты Америки");
				}
				if (strValue.equals("Союз Советских Социалистических Республик") || strValue.equals("СССР")) {
					return fakeEntity(Places.СССР, "Союз Советских Социалистических Республик");
				}
				return null;
			}

			private Entity fakeEntity(EntityId id, String title) {
				final Entity result = new Entity(new JSONObject());
				result.setId(id);
				result.putLabel(new Label("ru", title));
				return result;
			}

		};
	}

	private PlacesHelper mockPlacesHelper() {
		final TreeMap<EntityId, List<Statement>> hasCategories = new TreeMap<EntityId, List<Statement>>();
		hasCategories.put(Places.Кишинёв, Collections.emptyList());
		hasCategories.put(Places.Лос_Анджелес, Collections.emptyList());
		hasCategories.put(Places.Москва, Collections.emptyList());
		hasCategories.put(Places.Нью_Йорк_город, Collections.emptyList());
		hasCategories.put(Places.Санкт_Петербург, Collections.emptyList());
		hasCategories.put(Places.Ростов_на_Дону, Collections.emptyList());

		final PlacesHelper placesHelper = new PlacesHelper();
		placesHelper.onUpdate(Properties.CATEGORY_FOR_PEOPLE_BORN_HERE, hasCategories);
		placesHelper.onUpdate(Properties.CATEGORY_FOR_PEOPLE_DIED_HERE, hasCategories);

		placesHelper.setCountries(new HashSet<>(Arrays.asList(Places.Российская_империя, Places.Россия, Places.СССР,
				Places.США)));

		return placesHelper;
	}

	@Test
	public void testParse() {

		final PlacesHelper placesHelper = mockPlacesHelper();
		final EntityByLinkResolver entityByLinkResolver = mockEntityByLinkResolver();

		Assert.assertEquals(
				Places.Кишинёв.toWikilink(true)
						+ "\n" //
						+ "* " + Properties.ADMINISTRATIVE_UNIT.toWikilink(true) + " → " + Places.МССР.toWikilink(true)
						+ "\n" //
						+ "* " + Properties.COUNTRY.toWikilink(true) + " → " + Places.СССР.toWikilink(true),
				placesHelper
						.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH,
								"{{МестоРождения|Кишинёв}}, [[МССР]], [[СССР]]").get(0).toString(x -> x.toString(), 0));

		Assert.assertEquals(
				Places.Лос_Анджелес.toWikilink(true)
						+ "\n" //
						+ "* " + Properties.ADMINISTRATIVE_UNIT.toWikilink(true) + " → "
						+ Places.Калифорния.toWikilink(true) + "\n" //
						+ "* " + Properties.COUNTRY.toWikilink(true) + " → " + Places.США.toWikilink(true),
				placesHelper
						.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH,
								"[[Лос-Анджелес]], [[Калифорния]], [[Соединённые Штаты Америки]]").get(0)
						.toString(x -> x.toString(), 0));
		Assert.assertEquals(
				Places.Лос_Анджелес.toWikilink(true)
						+ "\n" //
						+ "* " + Properties.ADMINISTRATIVE_UNIT.toWikilink(true) + " → "
						+ Places.Калифорния.toWikilink(true) + "\n" //
						+ "* " + Properties.COUNTRY.toWikilink(true) + " → " + Places.США.toWikilink(true),
				placesHelper
						.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH,
								"{{МестоРождения|Лос-Анджелес}}, [[Калифорния]], [[Соединённые Штаты Америки|США]]")
						.get(0).toString(x -> x.toString(), 0));

		Assert.assertEquals(
				"[[:d:Q649|Q649]]",
				placesHelper.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH, "[[Москва]]").get(0)
						.toString(x -> x.toString(), 0));

		Assert.assertEquals("[[:d:Q649|Q649]]\n" //
				+ "* [[:d:Property:P131|P131]] → [[:d:Q2184|Q2184]]\n" //
				+ "* [[:d:Property:P17|P17]] → [[:d:Q15180|Q15180]]",
				placesHelper.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH, "[[Москва]], [[РСФСР]], [[СССР]]")
						.get(0).toString(x -> x.toString(), 0));

		Assert.assertEquals("[[:d:Q649|Q649]]",
				placesHelper.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH, "{{Место смерти|Москва}}").get(0)
						.toString(x -> x.toString(), 0));

		Assert.assertEquals("[[:d:Q649|Q649]]",
				placesHelper.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH, "{{Место смерти|Москва|в Москве}}")
						.get(0).toString(x -> x.toString(), 0));

		Assert.assertEquals(
				"[[:d:Q60|Q60]]\n" + "* [[:d:Property:P17|P17]] → [[:d:Q30|Q30]]",
				placesHelper
						.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH,
								"{{МестоРождения|Нью-Йорк}}, [[Соединённые Штаты Америки|США]]").get(0)
						.toString(x -> x.toString(), 0));
		Assert.assertEquals("[[:d:Q60|Q60]]\n" + "* [[:d:Property:P17|P17]] → [[:d:Q30|Q30]]",
				placesHelper.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH, "[[Нью-Йорк]], США").get(0)
						.toString(x -> x.toString(), 0));
		Assert.assertEquals(
				Places.Нью_Йорк_город.toWikilink(true)
						+ "\n" //
						+ "* " + Properties.ADMINISTRATIVE_UNIT.toWikilink(true) + " → "
						+ Places.Нью_Йорк_штат.toWikilink(true) + "\n" //
						+ "* " + Properties.COUNTRY.toWikilink(true) + " → " + Places.США.toWikilink(true),
				placesHelper
						.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH,
								"[[Нью-Йорк]], [[Нью-Йорк (штат)|Нью-Йорк]], [[Соединённые Штаты Америки]]").get(0)
						.toString(x -> x.toString(), 0));

		Assert.assertEquals(
				"[[:d:Q656|Q656]]\n" + "* [[:d:Property:P17|P17]] → [[:d:Q34266|Q34266]]",
				placesHelper
						.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH,
								"{{Место рождения|Санкт-Петербург}}, [[Российская империя]]").get(0)
						.toString(x -> x.toString(), 0));

		Assert.assertEquals(
				"[[:d:Q656|Q656]]\n" + "* [[:d:Property:P17|P17]] → [[:d:Q34266|Q34266]]",
				placesHelper
						.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH,
								"{{МестоРождения|Санкт-Петербург}},<br /> [[Российская империя]]").get(0)
						.toString(x -> x.toString(), 0));
		try {
			Assert.assertEquals(
					Places.Санкт_Петербург.toWikilink(true) + "\n" //
							+ "* " + Properties.COUNTRY.toWikilink(true) + " → " + Places.СССР.toWikilink(true),
					placesHelper
							.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH,
									"{{МестоРождения|Ленинград|в Санкт-Петербурге|Санкт-Петербург}}, [[Союз Советских Социалистических Республик]]")
							.get(0).toString(x -> x.toString(), 0));
			Assert.fail("NotSameLabelException expected");
		} catch (NotSameLabelException exc) {
			// success
		}

		Assert.assertEquals(
				Places.СССР.toWikilink(true),
				placesHelper.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH, "[[СССР]]").get(0)
						.toString(x -> x.toString(), 0));

		Assert.assertEquals(
				Places.США.toWikilink(true),
				placesHelper.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH, "[[США]]").get(0)
						.toString(x -> x.toString(), 0));

		Assert.assertEquals(
				Places.Ростов_на_Дону.toWikilink(true),
				placesHelper
						.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH,
								"{{МР|Ростов-на-Дону|в Ростове-на-Дону}}").get(0).toString(x -> x.toString(), 0));

		try {
			Assert.assertEquals(
					"[[:d:Q649|Q649]]",
					placesHelper.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH, "{{Место смерти|Одесса}}")
							.get(0).toString(x -> x.toString(), 0));
			Assert.fail("AutocategorizationRequiredException required");
		} catch (AutocategorizationRequiredException exc) {
			// success
		}

	}

}
