package org.wikipedia.vlsergey.secretary.trust;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Test;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

public class RevisionAuthorshipCalculatorTest {

	@Test
	public void testCalculator() throws IOException {
		final Locale locale = new Locale("ru-RU");

		final RevisionAuthorshipCalculator calculator = new RevisionAuthorshipCalculator();
		calculator.setProject(Project.RUWIKIPEDIA);

		String text1 = IoUtils.readToString(
				RevisionAuthorshipCalculatorTest.class.getResourceAsStream("Санкт-Петербург-1.txt"), "utf-8");
		String text2 = IoUtils.readToString(
				RevisionAuthorshipCalculatorTest.class.getResourceAsStream("Санкт-Петербург-2.txt"), "utf-8");

		TextChunkList chunks1 = TextChunkList.toTextChunkList(locale, "A", text1);
		TextChunkList chunks2 = TextChunkList.toTextChunkList(locale, "B", text2);

		final TextChunkList joined = calculator.join(chunks1, chunks2);
		Assert.assertEquals("A 97,21%; B 2,79%;", calculator.toString(joined));
	}

	@Test
	public void testDifference() throws IOException {
		final Locale locale = new Locale("ru-RU");

		String text1 = IoUtils.readToString(
				RevisionAuthorshipCalculatorTest.class.getResourceAsStream("Санкт-Петербург-1.txt"), "utf-8");
		String text2 = IoUtils.readToString(
				RevisionAuthorshipCalculatorTest.class.getResourceAsStream("Санкт-Петербург-2.txt"), "utf-8");

		TextChunkList chunks1 = TextChunkList.toTextChunkList(locale, "A", text1);
		TextChunkList chunks2 = TextChunkList.toTextChunkList(locale, "B", text2);

		Assert.assertEquals(8857, TextChunkList.calculateDifference(chunks1, chunks2));
	}

	@Test
	public void testJoin() {
		final Locale locale = new Locale("ru-RU");

		final RevisionAuthorshipCalculator calculator = new RevisionAuthorshipCalculator();
		calculator.setProject(Project.RUWIKIPEDIA);

		TextChunkList baseRevision = TextChunkList.concatenate(Arrays.asList(new TextChunkList[] {
				TextChunkList.toTextChunkList(locale, "A", "0 1 2 3 4"),
				TextChunkList.toTextChunkList(locale, "B", "5 6 7 8 9") }));

		TextChunkList newRevision = TextChunkList.toTextChunkList(locale, "C", "a a 1 2 3 4 5 6 7 e e 8 9 z z");

		TextChunkList joined = calculator.join(baseRevision, newRevision);

		Assert.assertEquals("C 40%; B 33,33%; A 26,67%;", calculator.toString(joined));
	}

}
