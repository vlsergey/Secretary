package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;

class AbstractHelper {

	static final List<Snak> CIRCUMSTANCES_CIRCA = Collections.singletonList(Snak.newSnak(
			Properties.SOURCING_CIRCUMSTANCES, SourcingCircumstances.CIRCA));

	static final Set<String> UNKNOWN = new HashSet<>(Arrays.asList("?", "неизвестно", "неизвестна", "не установлено"));

	public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipediaSnaks,
			Collection<ValueWithQualifiers> wikidataSnaks) {

		if (wikipediaSnaks.isEmpty()) {
			return ReconsiliationAction.remove_from_wikipedia_as_empty;
		}
		if (wikidataSnaks.isEmpty()) {
			return ReconsiliationAction.set;
		}

		if (wikidataSnaks.containsAll(wikipediaSnaks)) {
			return ReconsiliationAction.remove_from_wikipedia_as_not_empty;
		}

		return ReconsiliationAction.report_difference;
	}

}
