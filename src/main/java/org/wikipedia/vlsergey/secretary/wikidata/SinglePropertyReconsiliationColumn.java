package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;

class SinglePropertyReconsiliationColumn extends ReconsiliationColumn {

	private final EntityId property;

	public SinglePropertyReconsiliationColumn(List<String> templateParameters, DataType dataType, EntityId property,
			Function<String, List<ValueWithQualifiers>> parseF) {
		super(templateParameters, dataType, new EntityId[] { property }, parseF);
		this.property = property;
	}

	@Override
	public void fillToWikidata(Collection<ValueWithQualifiers> source, JSONObject result,
			Map<EntityId, List<String>> claimIdsToBeDeletedMap) {
		List<String> claimIdsToBeDeleted = claimIdsToBeDeletedMap.get(this.property);
		fillToWikidata(this.property, source, result, claimIdsToBeDeleted);
	}

	@Override
	public List<ValueWithQualifiers> fromWikidata(Entity entity, boolean ignoreRuwikiImport) {
		return fromWikidata(entity, this.property, ignoreRuwikiImport);
	}

	@Override
	public String getCode() {
		return property.toString();
	}
}