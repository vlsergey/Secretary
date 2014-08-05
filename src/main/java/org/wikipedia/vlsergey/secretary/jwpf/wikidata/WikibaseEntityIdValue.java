package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class WikibaseEntityIdValue extends DataValue {

	public static final String DATATYPE = "wikibase-item";

	private static final String KEY_ENTITY_TYPE = "entity-type";
	private static final String KEY_NUMERIC_ID = "numeric-id";

	public WikibaseEntityIdValue(EntityId entityId) {
		super(new JSONObject());

		jsonObject.put(KEY_TYPE, "wikibase-entityid");

		final JSONObject value = new JSONObject();
		value.put(KEY_ENTITY_TYPE, entityId.getType().name());
		value.put(KEY_NUMERIC_ID, entityId.getId());
	}

	protected WikibaseEntityIdValue(JSONObject jsonObject) {
		super(jsonObject);
	}

	public Long getNumericId() {
		return jsonObject.getJSONObject(KEY_VALUE).getLong(KEY_NUMERIC_ID);
	}

}
