package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.Locale;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Text;

public class WikibaseEntityIdValue extends DataValue {

	private static final String KEY_ENTITY_TYPE = "entity-type";
	private static final String KEY_NUMERIC_ID = "numeric-id";

	public WikibaseEntityIdValue(EntityId entityId) {
		super(new JSONObject());

		jsonObject.put(KEY_TYPE, ValueType.WIKIBASE_ENTITYID.code);

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
				&& StringUtils.equalsIgnoreCase(this.getEntityType(), ((WikibaseEntityIdValue) obj).getEntityType())
				&& this.getNumericId().equals(((WikibaseEntityIdValue) obj).getNumericId());
	}

	public EntityId getEntityId() {
		String entityType = getEntityType();
		if (StringUtils.equalsIgnoreCase("item", entityType)) {
			return EntityId.item(getNumericId());
		}
		if (StringUtils.equalsIgnoreCase("property", entityType)) {
			return EntityId.property(getNumericId());
		}
		throw new RuntimeException("Unknown entity type: " + entityType);
	}

	public String getEntityType() {
		return jsonObject.getJSONObject(KEY_VALUE).getString(KEY_ENTITY_TYPE);
	}

	public Long getNumericId() {
		return jsonObject.getJSONObject(KEY_VALUE).getLong(KEY_NUMERIC_ID);
	}

	@Override
	public int hashCode() {
		return getNumericId().hashCode();
	}

	@Override
	public Content toWiki(Locale locale, Function<EntityId, String> labelResolver) {
		return new Text("[[:d:Q" + getNumericId() + "|" + labelResolver.apply(getEntityId()) + "]]");
	}

}
