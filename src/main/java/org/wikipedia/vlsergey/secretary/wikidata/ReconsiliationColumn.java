package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiSnak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.StringValue;

class ReconsiliationColumn {

	final DataType dataType;
	final EntityId property;
	final Collection<String> templateParameters;
	final Function<String, List<ValueWithQualifiers>> toWikidata;

	public ReconsiliationColumn(List<String> templateParameters, DataType dataType, EntityId property,
			Function<String, List<ValueWithQualifiers>> toWikidata) {
		this.templateParameters = templateParameters;
		this.dataType = dataType;
		this.property = property;
		this.toWikidata = toWikidata;
	}

	public ReconsiliationColumn(String templateParameter, DataType dataType, EntityId property) {
		this.templateParameters = Collections.singletonList(templateParameter);
		this.dataType = dataType;
		this.property = property;
		this.toWikidata = x -> Collections.singletonList(new ValueWithQualifiers(ApiSnak.newSnak(property, dataType,
				new StringValue(x)), Collections.emptyList()));
	}

	public ReconsiliationColumn(String templateParameter, DataType dataType, EntityId property,
			Function<String, List<ValueWithQualifiers>> toWikidata) {
		this.templateParameters = Collections.singletonList(templateParameter);
		this.dataType = dataType;
		this.property = property;
		this.toWikidata = toWikidata;
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