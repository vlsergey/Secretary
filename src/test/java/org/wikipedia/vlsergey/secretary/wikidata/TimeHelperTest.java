package org.wikipedia.vlsergey.secretary.wikidata;

import java.time.OffsetDateTime;
import java.time.temporal.JulianFields;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;

public class TimeHelperTest {

	@Test
	public void testFixJulianToGrigorian() {

		Assert.assertEquals("-0900-02-19T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0900-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("-0900-02-21T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0900-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("-0500-02-24T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-01T00:00:00.00Z")).toString());
		Assert.assertEquals("-0500-02-25T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-02T00:00:00.00Z")).toString());
		Assert.assertEquals("-0500-02-26T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-03T00:00:00.00Z")).toString());
		Assert.assertEquals("-0500-02-27T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-04T00:00:00.00Z")).toString());
		Assert.assertEquals("-0500-02-28T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-05T00:00:00.00Z")).toString());
		Assert.assertEquals("-0500-03-01T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0500-03-06T00:00:00.00Z")).toString());

		Assert.assertEquals("-0300-02-27T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0300-03-03T00:00:00.00Z")).toString());
		Assert.assertEquals("-0300-02-28T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0300-03-04T00:00:00.00Z")).toString());

		Assert.assertEquals("-0200-02-24T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0200-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("-0200-02-26T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0200-03-01T00:00:00.00Z")).toString());
		Assert.assertEquals("-0200-02-27T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0200-03-02T00:00:00.00Z")).toString());
		Assert.assertEquals("-0200-03-01T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0200-03-04T00:00:00.00Z")).toString());

		Assert.assertEquals("-0100-02-25T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0100-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("-0100-02-27T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0100-03-01T00:00:00.00Z")).toString());
		Assert.assertEquals("-0100-02-28T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0100-03-02T00:00:00.00Z")).toString());
		Assert.assertEquals("-0100-03-01T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("-0100-03-03T00:00:00.00Z")).toString());

		Assert.assertEquals("0100-02-26T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("0100-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("0100-02-28T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("0100-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("0200-02-27T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("0200-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("0200-03-01T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("0200-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("0300-02-28T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("0300-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("0300-03-02T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("0300-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("1000-03-05T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1000-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("1000-03-07T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1000-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("1100-03-06T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1100-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("1100-03-08T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1100-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("1200-03-06T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1200-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("1200-03-07T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1200-02-29T00:00:00.00Z")).toString());
		Assert.assertEquals("1200-03-08T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1200-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("1300-03-07T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1300-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("1300-03-09T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1300-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("1400-03-08T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1400-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("1400-03-10T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1400-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("1500-03-09T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1500-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("1500-03-11T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1500-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("1600-03-09T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1600-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("1600-03-10T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1600-02-29T00:00:00.00Z")).toString());
		Assert.assertEquals("1600-03-11T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1600-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("1700-03-10T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1700-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("1700-03-12T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1700-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("1800-03-11T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1800-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("1800-03-13T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1800-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("1900-03-12T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1900-02-28T00:00:00.00Z")).toString());
		Assert.assertEquals("1900-03-14T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("1900-03-01T00:00:00.00Z")).toString());

		Assert.assertEquals("2100-02-28T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("2100-02-15T00:00:00.00Z")).toString());
		Assert.assertEquals("2100-03-01T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("2100-02-16T00:00:00.00Z")).toString());
		Assert.assertEquals("2100-03-13T00:00Z[UTC]",
				TimeHelper.fixJulianToGrigorian(OffsetDateTime.parse("2100-02-28T00:00:00.00Z")).toString());
	}

	@Test
	public void testJulianDay() {
		{
			OffsetDateTime dateTime = OffsetDateTime.parse("1814-10-15T00:00:00.00Z");
			Assert.assertEquals(2383897, dateTime.getLong(JulianFields.JULIAN_DAY));
			Assert.assertEquals(-16104, dateTime.getLong(JulianFields.MODIFIED_JULIAN_DAY));
		}
		{
			OffsetDateTime dateTime = OffsetDateTime.parse("0200-02-28T00:00:00.00Z");
			Assert.assertEquals(1794167, dateTime.getLong(JulianFields.JULIAN_DAY));
			Assert.assertEquals(-605834, dateTime.getLong(JulianFields.MODIFIED_JULIAN_DAY));
		}
		{
			// -200-02-29 in Julian
			OffsetDateTime dateTime = OffsetDateTime.parse("-0200-02-25T00:00:00.00Z");
			Assert.assertEquals(1648067, dateTime.getLong(JulianFields.JULIAN_DAY));
			Assert.assertEquals(-751934, dateTime.getLong(JulianFields.MODIFIED_JULIAN_DAY));
		}
	}

	@Test
	public void testParse() {
		Assert.assertEquals(
				"{\"datatype\":\"time\",\"datavalue\":{\"type\":\"time\",\"value\":{\"before\":0,\"timezone\":0,\"precision\":7,\"time\":\"-00000000400-01-01T00:00:00Z\",\"after\":0,\"calendarmodel\":\"http://www.wikidata.org/entity/Q1985727\"}},\"property\":\"P569\",\"snaktype\":\"value\"}",
				new TimeHelper().parse(Properties.DATE_OF_BIRTH, "4 век до н.э.").get(0).getJsonObject().toString());
		Assert.assertEquals(
				"{\"datatype\":\"time\",\"datavalue\":{\"type\":\"time\",\"value\":{\"before\":0,\"timezone\":0,\"precision\":7,\"time\":\"+00000001600-01-01T00:00:00Z\",\"after\":0,\"calendarmodel\":\"http://www.wikidata.org/entity/Q1985727\"}},\"property\":\"P569\",\"snaktype\":\"value\"}",
				new TimeHelper().parse(Properties.DATE_OF_BIRTH, "[[XVI век]]").get(0).getJsonObject().toString());
	}
}
