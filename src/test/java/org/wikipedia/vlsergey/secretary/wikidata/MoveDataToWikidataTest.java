package org.wikipedia.vlsergey.secretary.wikidata;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.TimeValue;

public class MoveDataToWikidataTest {

	@Test
	public void testDateParse() {
		Assert.assertEquals("+00000003456-02-01T00:00:00Z",
				((TimeValue) MoveDataToWikidata.parseDateFunction.parse("01.02.3456").iterator().next())
						.getTimeString());
		Assert.assertEquals("-00000003456-02-01T00:00:00Z",
				((TimeValue) MoveDataToWikidata.parseDateFunction.parse("01.02.-3456").iterator().next())
						.getTimeString());
		Assert.assertEquals("+00000000123-01-01T00:00:00Z",
				((TimeValue) MoveDataToWikidata.parseDateFunction.parse("123").iterator().next()).getTimeString());
		Assert.assertEquals("-00000000123-01-01T00:00:00Z",
				((TimeValue) MoveDataToWikidata.parseDateFunction.parse("-123").iterator().next()).getTimeString());
	}

}
