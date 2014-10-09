package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;

class ReconsiliationColumn {

	final DataType dataType;
	final Function<String, List<ValueWithQualifiers>> parseF;
	final EntityId property;
	final Collection<String> templateParameters;

	public ReconsiliationColumn(List<String> templateParameters, DataType dataType, EntityId property,
			Function<String, List<ValueWithQualifiers>> parseF) {
		this.templateParameters = templateParameters;
		this.dataType = dataType;
		this.property = property;
		this.parseF = parseF;
	}

	public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipedia,
			Collection<ValueWithQualifiers> wikidata) {
		if (wikipedia.isEmpty()) {
			return ReconsiliationAction.remove_from_wikipedia;
		}
		if (wikidata.isEmpty()) {
			return ReconsiliationAction.set;
		}
		if (wikidata.containsAll(wikipedia)) {
			return ReconsiliationAction.remove_from_wikipedia;
		}
		return ReconsiliationAction.report_difference;
	}
}