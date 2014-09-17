package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiSnak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;

class AbstractHelper {

	public ReconsiliationAction getAction(Collection<ApiSnak> wikipediaSnaks, Collection<ApiSnak> wikidataSnaks) {

		if (wikipediaSnaks.isEmpty()) {
			return ReconsiliationAction.remove_from_wikipedia;
		}
		if (wikidataSnaks.isEmpty()) {
			return ReconsiliationAction.set;
		}

		List<DataValue> wikipedia = wikipediaSnaks.stream()
				.map(x -> x.getSnakType() == SnakType.value ? x.getDataValue() : null).collect(Collectors.toList());
		List<DataValue> wikidata = wikidataSnaks.stream()
				.map(x -> x.getSnakType() == SnakType.value ? x.getDataValue() : null).collect(Collectors.toList());

		if (wikidata.containsAll(wikipedia)) {
			return ReconsiliationAction.remove_from_wikipedia;
		}
		return ReconsiliationAction.report_difference;
	}

}
