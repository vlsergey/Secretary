package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class CountriesHelperTest {

	@Test
	public void testNormalize() {
		final CountriesHelper countriesHelper = new CountriesHelper();

		Assert.assertEquals(Arrays.asList("Белоруссия"), countriesHelper.normalize("{{Белоруссия}}"));
		Assert.assertEquals(Arrays.asList("Белоруссия"), countriesHelper.normalize("{{Белоруссия}} [[Белоруссия]]"));
		Assert.assertEquals(Arrays.asList("Германия", "США"), countriesHelper.normalize("Германия, США"));
		Assert.assertEquals(Arrays.asList("Королевство Франция"),
				countriesHelper.normalize("[[Файл:Pavillon royal de France.svg|border|22px]] [[Королевство Франция]] "));
		Assert.assertEquals(Arrays.asList("Украина"), countriesHelper.normalize("{{Флаг|Украина}} [[Украина]]"));
		Assert.assertEquals(
				Arrays.asList("Османская империя", "Армянская ССР"),
				countriesHelper
						.normalize("{{Флаг|Османская империя}} [[Османская империя]]<br /> {{Флаг|Армянская ССР}} [[Армянская ССР]]"));

		Assert.assertEquals(Arrays.asList("Российская империя", "Швейцария"),
				countriesHelper.normalize("{{Флагификация|Российская империя}} → {{SUI}}"));

	}

}
