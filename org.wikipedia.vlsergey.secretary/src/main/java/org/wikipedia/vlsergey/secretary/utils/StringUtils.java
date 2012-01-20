package org.wikipedia.vlsergey.secretary.utils;

import java.util.ArrayList;
import java.util.Collection;

import org.wikipedia.vlsergey.secretary.functions.Function;

public class StringUtils extends org.apache.commons.lang.StringUtils {
	public static Function<Collection<String>, Collection<String>> toLowerCaseF() {
		return new Function<Collection<String>, Collection<String>>() {
			@Override
			public Collection<String> apply(Collection<String> source) {
				ArrayList<String> result = new ArrayList<String>(source.size());
				for (String original : source) {
					if (original == null) {
						result.add(null);
						continue;
					}
					result.add(original.toLowerCase());
				}
				return result;
			}
		};
	}
}
