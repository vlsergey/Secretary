package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public interface Snak {

	DataType getDataType();

	default DataValue getDataValue() {
		switch (getDataType().getValueType()) {
		case string:
			return getStringValue();
		case time:
			return getTimeValue();
		case wikibase_entityid:
			return getWikibaseEntityIdValue();
		default:
			throw new UnsupportedOperationException("Unsupported value type: " + getDataType().getValueType());
		}
	}

	String getHash();

	EntityId getProperty();

	SnakType getSnakType();

	StringValue getStringValue();

	TimeValue getTimeValue();

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