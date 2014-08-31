package org.wikipedia.vlsergey.secretary.wikidata;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Assert;
import org.junit.Test;

public class TimeHelperTest {

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("uuuu'-'MM'-'dd");

	@Test
	public void testFixJulianToGrigorian() {

		Assert.assertEquals("-0500-02-24",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-01T00:00:00.00Z"))));
		Assert.assertEquals("-0500-02-25",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-02T00:00:00.00Z"))));
		Assert.assertEquals("-0500-02-26",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-03T00:00:00.00Z"))));
		Assert.assertEquals("-0500-02-27",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-04T00:00:00.00Z"))));
		Assert.assertEquals("-0500-02-28",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-05T00:00:00.00Z"))));
		Assert.assertEquals("-0500-03-01",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-06T00:00:00.00Z"))));

		Assert.assertEquals("-0300-02-27",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0300-03-03T00:00:00.00Z"))));
		Assert.assertEquals("-0300-02-28",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0300-03-04T00:00:00.00Z"))));

		Assert.assertEquals("-0200-02-24",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0200-02-28T00:00:00.00Z"))));
		Assert.assertEquals("-0200-02-26",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0200-03-01T00:00:00.00Z"))));
		Assert.assertEquals("-0200-02-27",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0200-03-02T00:00:00.00Z"))));
		Assert.assertEquals("-0200-03-01",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0200-03-04T00:00:00.00Z"))));

		Assert.assertEquals("-0100-02-25",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0100-02-28T00:00:00.00Z"))));
		Assert.assertEquals("-0100-02-27",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0100-03-01T00:00:00.00Z"))));
		Assert.assertEquals("-0100-02-28",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0100-03-02T00:00:00.00Z"))));
		Assert.assertEquals("-0100-03-01",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0100-03-03T00:00:00.00Z"))));

		Assert.assertEquals("0100-02-26",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("0100-02-28T00:00:00.00Z"))));
		Assert.assertEquals("0100-02-28",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("0100-03-01T00:00:00.00Z"))));

		Assert.assertEquals("1900-03-12",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1900-02-28T00:00:00.00Z"))));
		Assert.assertEquals("1900-03-14",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1900-03-01T00:00:00.00Z"))));

		Assert.assertEquals("2100-02-28",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("2100-02-15T00:00:00.00Z"))));
		Assert.assertEquals("2100-03-01",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("2100-02-16T00:00:00.00Z"))));
		Assert.assertEquals("2100-03-13",
				formatter.format(TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("2100-02-28T00:00:00.00Z"))));
	}
}
