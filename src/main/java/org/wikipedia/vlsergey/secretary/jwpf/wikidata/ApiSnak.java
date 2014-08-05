package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class ApiSnak extends ApiValue implements Snak {

	public static final String KEY_DATATYPE = "datatype";

	public static final String KEY_DATAVALUE = "datavalue";

	public static final String KEY_HASH = "hash";

	public static final String KEY_PROPERTY = "property";

	public static final String KEY_SNAKTYPE = "snaktype";

	public static ApiSnak newStringValueSnak(EntityId property, String value) {
		ApiSnak apiSnak = new ApiSnak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(SnakType.value);
		apiSnak.setDatatype(StringValue.DATATYPE);
		apiSnak.setDatavalue(new StringValue(value));
		return apiSnak;
	}

	public static ApiSnak newWikibaseEntityIdValueSnak(EntityId property, EntityId entityId) {
		ApiSnak apiSnak = new ApiSnak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(SnakType.value);
		apiSnak.setDatatype(WikibaseEntityIdValue.DATATYPE);
		apiSnak.setDatavalue(new WikibaseEntityIdValue(entityId));
		return apiSnak;
	}

	public ApiSnak() {
		super(new JSONObject());
	}

	public ApiSnak(JSONObject jsonObject) {
		super(jsonObject);
	}

	@Override
	public String getDatatype() {
		return jsonObject.getString(KEY_DATATYPE);
	}

	@Override
	public String getHash() {
		return jsonObject.getString(KEY_HASH);
	}

	@Override
	public EntityId getProperty() {
		return EntityId.parse(jsonObject.getString(KEY_PROPERTY));
	}

	@Override
	public SnakType getSnakType() {
		return SnakType.valueOf(jsonObject.getString(KEY_SNAKTYPE));
	}

	@Override
	public StringValue getStringValue() {
		return new StringValue(jsonObject.getJSONObject(KEY_DATAVALUE));
	}

	@Override
	public WikibaseEntityIdValue getWikibaseEntityIdValue() {
		return new WikibaseEntityIdValue(jsonObject.getJSONObject(KEY_DATAVALUE));
	}

	@Override
	public boolean hasSnakType() {
		return jsonObject.has(KEY_SNAKTYPE);
	}

	@Override
	public void setDatatype(String value) {
		jsonObject.put(KEY_DATATYPE, value);
	}

	@Override
	public void setDatavalue(DataValue value) {
		jsonObject.put(KEY_DATAVALUE, value.jsonObject);
	}

	@Override
	public void setProperty(EntityId value) {
		jsonObject.put(KEY_PROPERTY, value);
	}

	@Override
	public void setSnakType(SnakType value) {
		jsonObject.put(KEY_SNAKTYPE, value.name());
	}

}
