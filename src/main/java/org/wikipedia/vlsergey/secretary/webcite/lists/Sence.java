package org.wikipedia.vlsergey.secretary.webcite.lists;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Sence extends SkipList {

	static final Set<String> HOSTS = new HashSet<String>(Arrays.asList(
	//

			"youtube.com",

			"wikinews.org",

			"wikipedia.org",

			"wikisource.org",

			"commons.wikimedia.org",

			"video.mail.ru",

			"rutube.ru",

			"maps.yandex.ru",

			"-"//
	));

	public static boolean contains(URI toTest) {
		return contains(HOSTS, toTest);
	}
}
