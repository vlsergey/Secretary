package org.wikipedia.vlsergey.secretary.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DateNormalizerTest {

	@Test
	public void testNormalizeDate() {

		DateNormalizer dateNormalizer = new DateNormalizer();

		assertEquals("2009-10-26", dateNormalizer.normalizeDate("26 October 2009"));
		assertEquals("2009-08-14", dateNormalizer.normalizeDate("14 августа 2009"));
		assertEquals("2012-06-22", dateNormalizer.normalizeDate("22.06.2012"));
		assertEquals("2012-03-20", dateNormalizer.normalizeDate("March 20, 2012"));

		assertEquals("2012-06-22", dateNormalizer.normalizeDate("22.06.12"));
		assertEquals("2010-03-06", dateNormalizer.normalizeDate("6&nbsp;Марта&nbsp;2010"));

		assertEquals("2012-01-25", dateNormalizer.normalizeDate("25 січня 2012"));

		assertEquals("2012-04-16", dateNormalizer.normalizeDate("{{Nobr|16 April}} 2012"));
		assertEquals("2008-11-23", dateNormalizer.normalizeDate("{{Nowrap|23 November}} 2008"));
		assertEquals("2010-02-16", dateNormalizer.normalizeDate("{{Start date|2010|2|16}}"));
		assertEquals("2011-11-17", dateNormalizer.normalizeDate("{{date|17|11|2011}}"));
		assertEquals("2011-03-24", dateNormalizer.normalizeDate("{{проверено|24|3|2011}}"));

	}

}
