package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Collection;

class AbstractHelper {

	public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipediaSnaks,
			Collection<ValueWithQualifiers> wikidataSnaks) {

		if (wikipediaSnaks.isEmpty()) {
			return ReconsiliationAction.remove_from_wikipedia;
		}
		if (wikidataSnaks.isEmpty()) {
			return ReconsiliationAction.set;
		}

		if (wikidataSnaks.containsAll(wikipediaSnaks)) {
			return ReconsiliationAction.remove_from_wikipedia;
		}

		return ReconsiliationAction.report_difference;
	}

}
