package org.wikipedia.vlsergey.secretary.trust.princeton;

/*************************************************************************
 *  Compilation:  javac SuffixArray.java
 *  Execution:    java SuffixArray < input.txt
 *  
 *  A data type that computes the suffix array of a string.
 *
 *  % java SuffixArray < abra.txt 
 *    i ind lcp rnk  select
 *  ---------------------------
 *    0  11   -   0  !
 *    1  10   0   1  A!
 *    2   7   1   2  ABRA!
 *    3   0   4   3  ABRACADABRA!
 *    4   3   1   4  ACADABRA!
 *    5   5   1   5  ADABRA!
 *    6   8   0   6  BRA!
 *    7   1   3   7  BRACADABRA!
 *    8   4   0   8  CADABRA!
 *    9   6   0   9  DABRA!
 *   10   9   0  10  RA!
 *   11   2   2  11  RACADABRA!
 *
 *  WARNING: This program assumes that the <tt>substring()</tt> method takes
 *  constant time and space. Beginning with Oracle / OpenJDK Java 7, Update 6,
 *  the substring method takes linear time and space in the size of the
 *  extracted substring. Do NOT use this code with such versions.
 *  
 *  See <a href = "http://java-performance.info/changes-to-string-java-1-7-0_06/">this article</a>
 *  for more details.
 *
 *************************************************************************/

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class SuffixArray<T> {

	// length of longest common prefix of s and t
	public static <T> int lcp(List<T> s, List<T> t) {
		int N = Math.min(s.size(), t.size());
		for (int i = 0; i < N; i++)
			if (!s.get(i).equals(t.get(i)))
				return i;
		return N;
	}

	private final int N;

	private final List<T>[] suffixes;

	public SuffixArray(Comparator<List<T>> comparator, List<T> s) {
		N = s.size();
		suffixes = new List[N];
		for (int i = 0; i < N; i++)
			suffixes[i] = s.subList(i, s.size());

		Arrays.parallelSort(suffixes, comparator);
	}

	// index of ith sorted suffix
	public int index(int i) {
		return N - suffixes[i].size();
	}

	// longest common prefix of suffixes(i) and suffixes(i-1)
	public int lcp(int i) {
		return lcp(suffixes[i], suffixes[i - 1]);
	}

	// longest common prefix of suffixes(i) and suffixes(j)
	public int lcp(int i, int j) {
		return lcp(suffixes[i], suffixes[j]);
	}

	// size of string
	public int length() {
		return N;
	}

	// ith sorted suffix
	public List<T> select(int i) {
		return suffixes[i];
	}

}
