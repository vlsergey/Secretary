package org.wikipedia.vlsergey.secretary.trust;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

public class RevisionAuthorshipCalculatorTest {

	@Test
	public void test() throws IOException {
		final RevisionAuthorshipCalculator calculator = new RevisionAuthorshipCalculator();
		calculator.setLocale(new Locale("ru-RU"));

		String text1 = IoUtils.readToString(
				RevisionAuthorshipCalculatorTest.class.getResourceAsStream("Санкт-Петербург-1.txt"), "utf-8");
		String text2 = IoUtils.readToString(
				RevisionAuthorshipCalculatorTest.class.getResourceAsStream("Санкт-Петербург-2.txt"), "utf-8");

		List<TextChunk> chunks1 = calculator.toChunks("A", text1);
		List<TextChunk> chunks2 = calculator.toChunks("B", text2);

		final List<TextChunk> joined = calculator.join(chunks1, chunks2);
		Assert.assertEquals("A 97,3%; B 2,7%;", calculator.toString(joined, false));
	}

	@Test
	public void testJoin() {
		final RevisionAuthorshipCalculator calculator = new RevisionAuthorshipCalculator();
		calculator.setLocale(new Locale("ru-RU"));

		List<TextChunk> baseRevision = new ArrayList<TextChunk>();
		baseRevision.addAll(calculator.toChunks("A", "0 1 2 3 4"));
		baseRevision.addAll(calculator.toChunks("B", "5 6 7 8 9"));

		List<TextChunk> newRevision = calculator.toChunks("C", "a a 1 2 3 4 5 6 7 e e 8 9 z z");

		List<TextChunk> joined = calculator.join(baseRevision, newRevision);

		Assert.assertEquals("C 40%; B 33,33%; A 26,67%;", calculator.toString(joined, false));
	}

	// @Test
	// public void testLongestSubstring() {
	// final RevisionAuthorshipCalculator calculator = new
	// RevisionAuthorshipCalculator();
	// calculator.setLocale(new Locale("ru-RU"));
	//
	// Assert.assertEquals("1", calculator.longestSubstring("1223", "1",
	// '-').toString());
	// Assert.assertEquals("567890", calculator.longestSubstring("1234567890",
	// "5678901234", '-').toString());
	// }
}
