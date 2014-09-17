package org.wikipedia.vlsergey.secretary.wikidata;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;

public class UrlHelperTest {

	@Test
	public void testParse() {

		Assert.assertEquals(
				"[{\"datatype\":\"string\",\"datavalue\":{\"type\":\"string\",\"value\":\"http://www.tempuri.org/\"},\"property\":\"P856\",\"snaktype\":\"value\"}]",
				new UrlHelper().parse(Properties.OFFICIAL_WEBSITE, "http://www.tempuri.org").toString());
		Assert.assertEquals(
				"[{\"datatype\":\"string\",\"datavalue\":{\"type\":\"string\",\"value\":\"http://www.tempuri.org/\"},\"property\":\"P856\",\"snaktype\":\"value\"}]",
				new UrlHelper().parse(Properties.OFFICIAL_WEBSITE, "http://www.tempuri.org/").toString());

		Assert.assertEquals(
				"[{\"datatype\":\"string\",\"datavalue\":{\"type\":\"string\",\"value\":\"http://www.tempuri.org/\"},\"property\":\"P856\",\"snaktype\":\"value\"}]",
				new UrlHelper().parse(Properties.OFFICIAL_WEBSITE, "[http://www.tempuri.org/ www.tempuri.org]")
						.toString());
		Assert.assertEquals(
				"[{\"datatype\":\"string\",\"datavalue\":{\"type\":\"string\",\"value\":\"http://www.tempuri.org/\"},\"property\":\"P856\",\"snaktype\":\"value\"}]",
				new UrlHelper().parse(Properties.OFFICIAL_WEBSITE, "[http://www.tempuri.org/]").toString());
		Assert.assertEquals(
				"[{\"datatype\":\"string\",\"datavalue\":{\"type\":\"string\",\"value\":\"http://www.tempuri.org/\"},\"property\":\"P856\",\"snaktype\":\"value\"}]",
				new UrlHelper().parse(Properties.OFFICIAL_WEBSITE, "[http://www.tempuri.org/ 有限会社アールピーエム]").toString());
		Assert.assertEquals(
				"[{\"datatype\":\"string\",\"datavalue\":{\"type\":\"string\",\"value\":\"http://www.tempuri.org/test-path/\"},\"property\":\"P856\",\"snaktype\":\"value\"}]",
				new UrlHelper().parse(Properties.OFFICIAL_WEBSITE,
						"[http://www.tempuri.org/test-path/ Личная страница Абдраим Б.Ж.]").toString());
		Assert.assertEquals(
				"[{\"datatype\":\"string\",\"datavalue\":{\"type\":\"string\",\"value\":\"http://www.tempuri.org/\"},\"property\":\"P856\",\"snaktype\":\"value\"}]",
				new UrlHelper().parse(Properties.OFFICIAL_WEBSITE, "{{URL|http://www.tempuri.org/}}").toString());
	}

}
