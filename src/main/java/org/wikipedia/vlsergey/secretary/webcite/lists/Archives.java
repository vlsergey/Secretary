package org.wikipedia.vlsergey.secretary.webcite.lists;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Archives extends SkipList {

	static final Set<String> HOSTS = new HashSet<String>(Arrays.asList(
	//
			"wikiwix.com",

			"archive.org",

			"waybackmachine.org",

			"webcitation.org",

			"peeep.us"));

	public static boolean contains(URI toTest) {
		return contains(HOSTS, toTest);
	}
}
