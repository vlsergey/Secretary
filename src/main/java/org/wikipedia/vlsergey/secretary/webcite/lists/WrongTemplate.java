package org.wikipedia.vlsergey.secretary.webcite.lists;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class WrongTemplate extends SkipList {

	static final Set<String> HOSTS = new HashSet<String>(Arrays.asList(
	//

			"books.google.com.br",

			"portal.acm.org",

			"books.google.com",

			"nytimes.com",

			"books.google.de",

			"arxiv.org",

			"dic.academic.ru",

			"allkriminal.ru",

			"elibrary.ru",

			"books.google.ru",

			"kommersant.ru",

			"kp.ru",

			"lib.ru",

			"ng.ru",

			"sovsport.ru",

			"sport-express.ru",

			"xakep.ru",

			"lingvo.yandex.ru",

			"slovari.yandex.ru",

			"books.google.com.ua",

			"books.google.co.uk",

			"-"//
	));

	public static boolean contains(URI toTest) {
		return contains(HOSTS, toTest);
	}

}
