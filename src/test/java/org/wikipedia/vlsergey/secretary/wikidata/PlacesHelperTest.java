package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.List;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Label;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;

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
			public ApiEntity apply(String strValue) {
				if (strValue.equals("Москва")) {
					return fakeEntity(649, strValue);
				}
				if (strValue.equals("Одесса")) {
					return fakeEntity(1874, strValue);
				}
				if (strValue.equals("Санкт-Петербург")) {
					return fakeEntity(656, strValue);
				}
				return null;
			}

			private ApiEntity fakeEntity(long id, String title) {
				final ApiEntity result = new ApiEntity(new JSONObject());
				result.setId(EntityId.item(id));
				result.putLabel(new Label("ru", title));
				return result;
			}

		};
	}

	private PlacesHelper mockPlacesHelper() {
		return new PlacesHelper();
	}

	@Test
	public void testParse() {

		final PlacesHelper placesHelper = mockPlacesHelper();
		final EntityByLinkResolver entityByLinkResolver = mockEntityByLinkResolver();

		Assert.assertEquals(
				"[[:d:Q649|Q649]]",
				placesHelper.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH, "[[Москва]]").get(0)
						.toString(x -> x.toString(), 0));

		Assert.assertEquals("[[:d:Q649|Q649]]",
				placesHelper.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH, "{{Место смерти|Москва}}").get(0)
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
					"[[:d:Q649|Q649]]",
					placesHelper.parse(entityByLinkResolver, Properties.PLACE_OF_BIRTH, "{{Место смерти|Одесса}}")
							.get(0).toString(x -> x.toString(), 0));
			Assert.fail("AutocategorizationRequiredException required");
		} catch (AutocategorizationRequiredException exc) {
			// success
		}

	}

}
