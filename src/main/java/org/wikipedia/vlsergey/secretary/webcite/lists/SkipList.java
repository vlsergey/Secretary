package org.wikipedia.vlsergey.secretary.webcite.lists;

import java.net.URI;
import java.util.Collection;

abstract class SkipList {

	protected static boolean contains(Collection<String> hosts, URI toTest) {
		String host = toTest.getHost().trim().toLowerCase();

		if (hosts.contains(host)) {
			return true;
		}

		for (String fromList : hosts) {
			if (host.endsWith("." + fromList)) {
				return true;
			}
		}

		return false;
	}

}
