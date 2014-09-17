package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.time.OffsetDateTime;

import org.junit.Assert;
import org.junit.Test;

public class TimeValueTest {

	@Test
	public void testDateTimeFormatter() throws Exception {
		Assert.assertEquals("+00000001794-05-20T00:00:00Z",
				TimeValue.toISO(TimeValue.fromISO("+00000001794-05-20T00:00:00Z")));
		Assert.assertEquals("-00000001794-05-20T00:00:00Z",
				TimeValue.toISO(TimeValue.fromISO("-00000001794-05-20T00:00:00Z")));
	}

	@Test
	public void testFloor() throws Exception {
		{
			TimeValue timeValue = new TimeValue(11, OffsetDateTime.now());
			timeValue.setTimeString("+00000001794-05-20T00:00:00Z");
			Assert.assertEquals("1794-05-20T00:00Z", timeValue.floor().toString());
		}
		{
			TimeValue timeValue = new TimeValue(TimeValue.PRECISION_CENTURY, OffsetDateTime.now());
			timeValue.setTimeString("+00000001200-01-01T00:00:00Z");
			Assert.assertEquals("1101-01-01T00:00Z", timeValue.floor().toString());
		}
		{
			TimeValue timeValue = new TimeValue(TimeValue.PRECISION_CENTURY, OffsetDateTime.now());
			timeValue.setTimeString("+00000001300-01-01T00:00:00Z");
			Assert.assertEquals("1201-01-01T00:00Z", timeValue.floor().toString());
		}
		{
			TimeValue timeValue = new TimeValue(TimeValue.PRECISION_CENTURY, OffsetDateTime.now());
			timeValue.setTimeString("+00000001400-01-01T00:00:00Z");
			Assert.assertEquals("1301-01-01T00:00Z", timeValue.floor().toString());
		}
		{
			TimeValue timeValue = new TimeValue(TimeValue.PRECISION_YEAR, OffsetDateTime.now());
			timeValue.setTimeString("+00000001638-02-03T04:05:06Z");
			Assert.assertEquals("1638-01-01T00:00Z", timeValue.floor().toString());
		}
		{
			TimeValue timeValue = new TimeValue(TimeValue.PRECISION_DECADE, OffsetDateTime.now());
			timeValue.setTimeString("+00000001638-02-03T04:05:06Z");
			Assert.assertEquals("1631-01-01T00:00Z", timeValue.floor().toString());
		}
	}

}
