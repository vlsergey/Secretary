package org.wikipedia.vlsergey.secretary.webcite.lists;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Failure404 extends SkipList {

	static final Set<String> HOSTS = new HashSet<String>(Arrays.asList(
	//
			"www.google.com",

			"textual.ru",

			"nssdc.gsfc.nasa.gov",

			"mosgortrans.ru",

			"-"//
	));

	public static boolean contains(URI toTest) {
		return contains(HOSTS, toTest);
	}

}
