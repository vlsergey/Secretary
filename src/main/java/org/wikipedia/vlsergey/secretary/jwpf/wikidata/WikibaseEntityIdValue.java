package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Text;

public class WikibaseEntityIdValue extends DataValue {

	private static final String KEY_ENTITY_TYPE = "entity-type";
	private static final String KEY_NUMERIC_ID = "numeric-id";

	public WikibaseEntityIdValue(EntityId entityId) {
		super(new JSONObject());

		jsonObject.put(KEY_TYPE, ValueType.wikibase_entityid.toString());

		final JSONObject value = new JSONObject();
		value.put(KEY_ENTITY_TYPE, entityId.getType().name());
		value.put(KEY_NUMERIC_ID, entityId.getId());
		jsonObject.put(KEY_VALUE, value);
	}

	protected WikibaseEntityIdValue(JSONObject jsonObject) {
		super(jsonObject);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof WikibaseEntityIdValue
				&& this.getNumericId().equals(((WikibaseEntityIdValue) obj).getNumericId());
	}

	public Long getNumericId() {
		return jsonObject.getJSONObject(KEY_VALUE).getLong(KEY_NUMERIC_ID);
	}

	@Override
	public int hashCode() {
		return getNumericId().hashCode();
	}

	@Override
	public Content toWiki() {
		return new Text("[[:d:Q" + getNumericId() + "|Q" + getNumericId() + "]]");
	}

}
