package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public interface Snak {

	DataValue getAbstractDataValue();

	default DataValue getDataValue() {

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

	String getHash();

	EntityId getProperty();

	SnakType getSnakType();

	StringValue getStringValue();

	TimeValue getTimeValue();

	default ValueType getValueType() {
		if (!hasValue()) {
			throw new IllegalStateException("no value");
		}

		DataValue dataValue = getAbstractDataValue();
		ValueType valueType = dataValue.getValueType();
		return valueType;
	}

	WikibaseEntityIdValue getWikibaseEntityIdValue();

	boolean hasSnakType();

	default boolean hasValue() {
		return getSnakType() == SnakType.value;
	}

	void setDataType(DataType dataType);

	void setDatavalue(DataValue value);

	void setProperty(EntityId value);

	void setSnakType(SnakType value);

}