package org.wikipedia.vlsergey.secretary.webcite.lists;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This URL has been archived internally and can be made available for scholars
 * on request, but we cannot make it accessible on the web, because the
 * copyright holder (...) has asked us not to display the material. If you have
 * concerns about this individual not being the copyright holder, or if you
 * require access to the material in our dark archive for scholarly or legal
 * purposes, please contact us.
 */
public class Blacklisted extends SkipList {

	static final Set<String> HOSTS = new HashSet<String>(Arrays.asList(//
			//

			"timeshighereducation.co.uk" //

			//
			));

	public static boolean contains(URI toTest) {
		return contains(HOSTS, toTest);
	}

}
