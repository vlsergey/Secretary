package org.wikipedia.vlsergey.secretary.webcite.lists;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class Erros extends SkipList {

	static final Set<String> HOSTS = new HashSet<String>(Arrays.asList(
	//

			// http://www.webcitation.org/5w563Hk2c
			"billboard.com",

			// http://content.yudu.com/Library/A1ntfz/ITFAnnualReportAccou/resources/index.htm?referrerUrl=
			"content.yudu.com",

			// circular redirects at http://mob.hu/noc-history-1
			"mob.hu",

			// http://www.webcitation.org/5wAZdFTwc
			"beyond2020.cso.ie",

			// http://www.webcitation.org/5w7BcNTfc
			"logainm.ie",

			// http://www.webcitation.org/5w7BcNTfc
			"www.logainm.ie",

			// always 403 by HTTP checker
			"euskomedia.org"

	));

	public static boolean contains(URI toTest) {
		return contains(HOSTS, toTest);
	}

}
