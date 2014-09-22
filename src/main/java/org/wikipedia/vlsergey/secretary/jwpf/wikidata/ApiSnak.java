package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class ApiSnak extends ApiValue implements Snak {

	public static final String KEY_DATATYPE = "datatype";

	public static final String KEY_DATAVALUE = "datavalue";

	public static final String KEY_HASH = "hash";

	public static final String KEY_PROPERTY = "property";

	public static final String KEY_SNAKTYPE = "snaktype";

	public static ApiSnak newSnak(EntityId property, DataType dataType, DataValue value) {
		ApiSnak apiSnak = new ApiSnak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(SnakType.value);
		apiSnak.setDataType(dataType);
		apiSnak.setDatavalue(value);
		return apiSnak;
	}

	public static ApiSnak newSnak(EntityId property, EntityId entityId) {
		ApiSnak apiSnak = new ApiSnak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(SnakType.value);
		apiSnak.setDataType(DataType.WIKIBASE_ITEM);
		apiSnak.setDatavalue(new WikibaseEntityIdValue(entityId));
		return apiSnak;
	}

	public static ApiSnak newSnak(EntityId property, SnakType snakType) {
		if (snakType == SnakType.value) {
			throw new IllegalArgumentException();
		}

		ApiSnak apiSnak = new ApiSnak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(snakType);
		return apiSnak;
	}

	public static ApiSnak newSnak(EntityId property, String value) {
		ApiSnak apiSnak = new ApiSnak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(SnakType.value);
		apiSnak.setDataType(DataType.STRING);
		apiSnak.setDatavalue(new StringValue(value));
		return apiSnak;
	}

	public static ApiSnak newSnak(EntityId property, TimeValue value) {
		ApiSnak apiSnak = new ApiSnak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(SnakType.value);
		apiSnak.setDataType(DataType.TIME);
		apiSnak.setDatavalue(value);
		return apiSnak;
	}

	public ApiSnak() {
		super(new JSONObject());
	}

	public ApiSnak(JSONObject jsonObject) {
		super(jsonObject);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ApiSnak))
			return false;

		ApiSnak other = (ApiSnak) obj;

		return this.getProperty().equals(other.getProperty()) //
				&& this.getSnakType().equals(other.getSnakType()) //
				&& (this.getSnakType() == SnakType.value ? this.getDataValue().equals(other.getDataValue()) : true);
	}

	@Override
	public DataValue getAbstractDataValue() {
		return new DataValue(jsonObject.getJSONObject(KEY_DATAVALUE));
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
	public TimeValue getTimeValue() {
		return new TimeValue(jsonObject.getJSONObject(KEY_DATAVALUE));
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
	public void setDataType(DataType dataType) {
		jsonObject.put(KEY_DATATYPE, dataType.getDataType());
	}

	@Override
	public void setDatavalue(DataValue value) {
		jsonObject.put(KEY_DATAVALUE, value.jsonObject);
	}

	@Override
	public void setProperty(EntityId value) {
		jsonObject.put(KEY_PROPERTY, value.toString().toUpperCase());
	}

	@Override
	public void setSnakType(SnakType value) {
		jsonObject.put(KEY_SNAKTYPE, value.name());
	}

}
