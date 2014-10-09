package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class Snak extends Value {

	public static final String KEY_DATATYPE = "datatype";

	public static final String KEY_DATAVALUE = "datavalue";

	public static final String KEY_HASH = "hash";

	public static final String KEY_PROPERTY = "property";

	public static final String KEY_SNAKTYPE = "snaktype";

	public static Snak newSnak(EntityId property, DataType dataType, DataValue value) {
		Snak apiSnak = new Snak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(SnakType.value);
		apiSnak.setDataType(dataType);
		apiSnak.setDatavalue(value);
		return apiSnak;
	}

	public static Snak newSnak(EntityId property, EntityId entityId) {
		Snak apiSnak = new Snak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(SnakType.value);
		apiSnak.setDataType(DataType.WIKIBASE_ITEM);
		apiSnak.setDatavalue(new WikibaseEntityIdValue(entityId));
		return apiSnak;
	}

	public static Snak newSnak(EntityId property, SnakType snakType) {
		if (snakType == SnakType.value) {
			throw new IllegalArgumentException();
		}

		Snak apiSnak = new Snak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(snakType);
		return apiSnak;
	}

	public static Snak newSnak(EntityId property, String value) {
		Snak apiSnak = new Snak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(SnakType.value);
		apiSnak.setDataType(DataType.STRING);
		apiSnak.setDatavalue(new StringValue(value));
		return apiSnak;
	}

	public static Snak newSnak(EntityId property, TimeValue value) {
		Snak apiSnak = new Snak();
		apiSnak.setProperty(property);
		apiSnak.setSnakType(SnakType.value);
		apiSnak.setDataType(DataType.TIME);
		apiSnak.setDatavalue(value);
		return apiSnak;
	}

	public Snak() {
		super(new JSONObject());
	}

	public Snak(JSONObject jsonObject) {
		super(jsonObject);
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Snak))
			return false;

		Snak other = (Snak) obj;

		return this.getProperty().equals(other.getProperty()) //
				&& this.getSnakType().equals(other.getSnakType()) //
				&& (this.getSnakType() == SnakType.value ? this.getDataValue().equals(other.getDataValue()) : true);
	}

	public DataValue getAbstractDataValue() {
		return new DataValue(jsonObject.getJSONObject(KEY_DATAVALUE));
	}

	public DataValue getDataValue() {

		if (!hasValue()) {
			throw new IllegalStateException("no value");
		}

		ValueType valueType = getValueType();
		switch (valueType) {
		case STRING:
			return getStringValue();
		case TIME:
			return getTimeValue();
		case WIKIBASE_ENTITYID:
			return getWikibaseEntityIdValue();
		default:
			throw new UnsupportedOperationException("Unsupported value type: " + valueType);
		}
	}

	public String getHash() {
		return jsonObject.getString(KEY_HASH);
	}

	public EntityId getProperty() {
		return EntityId.parse(jsonObject.getString(KEY_PROPERTY));
	}

	public SnakType getSnakType() {
		return SnakType.valueOf(jsonObject.getString(KEY_SNAKTYPE));
	}

	public StringValue getStringValue() {
		return new StringValue(jsonObject.getJSONObject(KEY_DATAVALUE));
	}

	public TimeValue getTimeValue() {
		return new TimeValue(jsonObject.getJSONObject(KEY_DATAVALUE));
	}

	public ValueType getValueType() {
		if (!hasValue()) {
			throw new IllegalStateException("no value");
		}

		DataValue dataValue = getAbstractDataValue();
		ValueType valueType = dataValue.getValueType();
		return valueType;
	}

	public WikibaseEntityIdValue getWikibaseEntityIdValue() {
		return new WikibaseEntityIdValue(jsonObject.getJSONObject(KEY_DATAVALUE));
	}

	public boolean hasSnakType() {
		return jsonObject.has(KEY_SNAKTYPE);
	}

	public boolean hasValue() {
		return getSnakType() == SnakType.value;
	}

	public boolean isWikibaseEntityIdValue(EntityId entityId) {
		if (hasValue() && getAbstractDataValue().getType() == ValueType.WIKIBASE_ENTITYID) {
			return entityId.equals(getWikibaseEntityIdValue().getEntityId());
		}
		return false;
	}

	public void setDataType(DataType dataType) {
		jsonObject.put(KEY_DATATYPE, dataType.getDataType());
	}

	public void setDatavalue(DataValue value) {
		jsonObject.put(KEY_DATAVALUE, value.jsonObject);
	}

	public void setProperty(EntityId value) {
		jsonObject.put(KEY_PROPERTY, value.toString().toUpperCase());
	}

	public void setSnakType(SnakType value) {
		jsonObject.put(KEY_SNAKTYPE, value.name());
	}

}
