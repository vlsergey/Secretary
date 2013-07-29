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

			final List<T> prevItem = sa.select(i - 1);
			final List<T> currItem = sa.select(i);

			final int currItemSize = currItem.size();
			final int prevItemSize = prevItem.size();

			// adjacent suffixes both from second text string
			if (currItemSize <= N2 && prevItemSize <= N2)
				continue;

			// adjacent suffixes both from first text string
			if (currItemSize > N2 + 1 && prevItemSize > N2 + 1)
				continue;

			// check if it is possible to have longer substring
			if (currItemSize > substring.size() && prevItemSize > substring.size()) {
				// check if adjacent suffixes longer common substring
				int length = SuffixArray.lcp(currItem, prevItem);
				if (length > substring.size())
					substring = currItem.subList(0, length);
			}
		}

		return substring;
	}
}
