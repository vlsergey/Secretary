package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONArray;

public class NativeSnak extends NativeValue implements Snak {

	static int INDEX_DATAVALUE = 3;
	static int INDEX_PROPERTY = 1;
	static int INDEX_TYPE = 0;

	private final JSONArray jsonArray;

	public NativeSnak(final JSONArray jsonArray) {
		this.jsonArray = jsonArray;
	}

	@Override
	public DataType getDataType() {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public String getHash() {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public EntityId getProperty() {
		return EntityId.property(jsonArray.getLong(INDEX_PROPERTY));
	}

	@Override
	public SnakType getSnakType() {
		return SnakType.valueOf(jsonArray.getString(INDEX_TYPE));
	}

	@Override
	public StringValue getStringValue() {
		return new StringValue(jsonArray.getString(INDEX_DATAVALUE));
	}

	@Override
	public TimeValue getTimeValue() {
		return new TimeValue(jsonArray.getJSONObject(INDEX_DATAVALUE));
	}

	@Override
	public ValueType getType() {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public WikibaseEntityIdValue getWikibaseEntityIdValue() {
		return new WikibaseEntityIdValue(jsonArray.getJSONObject(INDEX_DATAVALUE));
	}

	@Override
	public boolean hasSnakType() {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setDataType(DataType dataType) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setDatavalue(DataValue value) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setProperty(EntityId value) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setSnakType(SnakType value) {
		throw new UnsupportedOperationException("NYI");
	}
}
