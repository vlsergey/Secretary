package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

public class CountriesHelperTest {

	@Test
	public void testNormalize() {
		final CountriesHelper countriesHelper = new CountriesHelper();

		Assert.assertEquals(Arrays.asList("Королевство Франция"),
				countriesHelper.normalize("[[Файл:Pavillon royal de France.svg|border|22px]] [[Королевство Франция]] "));
	}

}
