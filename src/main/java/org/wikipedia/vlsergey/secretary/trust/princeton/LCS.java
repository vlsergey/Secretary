package org.wikipedia.vlsergey.secretary.trust.princeton;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class LCS {

	public static <T> List<T> lcs(Comparator<List<T>> comparator, List<T> text1, List<T> text2, T nonpresent) {
		int N1 = text1.size();
		int N2 = text2.size();

		List<T> sum = new ArrayList<T>(text1.size() + 1 + text2.size());
		sum.addAll(text1);
		sum.add(nonpresent);
		sum.addAll(text2);

		SuffixArray<T> sa = new SuffixArray<T>(comparator, sum);
		int N = sa.length();

		List<T> substring = Collections.emptyList();
		for (int i = 1; i < sa.length(); i++) {

			// adjacent suffixes both from second text string
			if (sa.select(i).size() <= N2 && sa.select(i - 1).size() <= N2)
				continue;

			// adjacent suffixes both from first text string
			if (sa.select(i).size() > N2 + 1 && sa.select(i - 1).size() > N2 + 1)
				continue;

			// check if adjacent suffixes longer common substring
			int length = sa.lcp(i);
			if (length > substring.size())
				substring = sa.select(i).subList(0, length);
		}

		return substring;
	}
}
