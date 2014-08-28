package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.sql.Date;

import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;

public class TimeValueTest {

	@Test
	public void testFloor() throws Exception {
		{
			TimeValue timeValue = new TimeValue(new JSONObject());
			timeValue.setTimeString("+00000001794-05-20T00:00:00Z");
			timeValue.setPrecision(11);
			Assert.assertEquals("20 May 1794 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
		{
			TimeValue timeValue = new TimeValue(new JSONObject());
			timeValue.setTimeString("+00000001200-01-01T00:00:00Z");
			timeValue.setPrecision(7);
			Assert.assertEquals("1 Jan 1101 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
		{
			TimeValue timeValue = new TimeValue(new JSONObject());
			timeValue.setTimeString("+00000001300-01-01T00:00:00Z");
			timeValue.setPrecision(7);
			Assert.assertEquals("1 Jan 1201 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
		{
			TimeValue timeValue = new TimeValue(new JSONObject());
			timeValue.setTimeString("+00000001400-01-01T00:00:00Z");
			timeValue.setPrecision(7);
			Assert.assertEquals("1 Jan 1301 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
		{
			TimeValue timeValue = new TimeValue(new JSONObject());
			timeValue.setTimeString("+00000001638-02-03T04:05:06Z");
			timeValue.setPrecision(9);
			Assert.assertEquals("1 Jan 1638 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
		{
			TimeValue timeValue = new TimeValue(new JSONObject());
			timeValue.setTimeString("+00000001638-02-03T04:05:06Z");
			timeValue.setPrecision(8);
			Assert.assertEquals("1 Jan 1631 00:00:00 GMT", new Date(timeValue.floor()).toGMTString());
		}
	}

}
