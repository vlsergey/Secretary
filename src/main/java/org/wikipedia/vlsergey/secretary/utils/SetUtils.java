package org.wikipedia.vlsergey.secretary.utils;

import java.util.HashSet;
import java.util.Set;

public class SetUtils extends org.apache.commons.collections.SetUtils {

	public static final <T> Set<T> intersection(Set<T> set1, Set<T> set2) {
		Set<T> result = new HashSet<T>();

		if (set1.size() > set2.size()) {
			Set<T> temp = set1;
			set1 = set2;
			set2 = temp;
		}

		for (T t : set1) {
			if (set2.contains(t)) {
				result.add(t);
			}
		}

		return result;
	}
}
