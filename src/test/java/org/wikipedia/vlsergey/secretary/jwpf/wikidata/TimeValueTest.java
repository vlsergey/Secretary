package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.Date;

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
			TimeValue timeValue = new TimeValue(11, new Date());
			timeValue.setTimeString("+00000001794-05-20T00:00:00Z");
			Assert.assertEquals("20 May 1794 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
		{
			TimeValue timeValue = new TimeValue(7, new Date());
			timeValue.setTimeString("+00000001200-01-01T00:00:00Z");
			Assert.assertEquals("1 Jan 1101 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
		{
			TimeValue timeValue = new TimeValue(7, new Date());
			timeValue.setTimeString("+00000001300-01-01T00:00:00Z");
			Assert.assertEquals("1 Jan 1201 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
		{
			TimeValue timeValue = new TimeValue(7, new Date());
			timeValue.setTimeString("+00000001400-01-01T00:00:00Z");
			Assert.assertEquals("1 Jan 1301 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
		{
			TimeValue timeValue = new TimeValue(9, new Date());
			timeValue.setTimeString("+00000001638-02-03T04:05:06Z");
			Assert.assertEquals("1 Jan 1638 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
		{
			TimeValue timeValue = new TimeValue(8, new Date());
			timeValue.setTimeString("+00000001638-02-03T04:05:06Z");
			Assert.assertEquals("1 Jan 1631 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
	}

}
