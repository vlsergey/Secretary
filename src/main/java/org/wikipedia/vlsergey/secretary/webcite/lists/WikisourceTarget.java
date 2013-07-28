package org.wikipedia.vlsergey.secretary.webcite.lists;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class WikisourceTarget extends SkipList {

	static final Set<String> HOSTS = new HashSet<String>(Arrays.asList( //
			//

			"bestpravo.ru",

			"docs.cntd.ru",

			"consultant.ru",

			"garant.ru",

			"protect.gost.ru",

			"pravo.gov.ru",

			"docs.kodeks.ru",

			"document.kremlin.ru",

			"zakon.scli.ru",

			"systema.ru",

			"-" //
	));

	public static boolean contains(URI toTest) {
		return contains(HOSTS, toTest);
	}

}
