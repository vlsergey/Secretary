package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.apache.commons.lang.StringUtils;

public enum DataType {

	STRING("string", ValueType.string),

	TIME("time", ValueType.time),

	URL("url", ValueType.string),

	WIKIBASE_ITEM("wikibase-item", ValueType.wikibase_entityid),

	;

	public static DataType get(String value) {
		for (DataType dataType : values()) {
			if (StringUtils.equalsIgnoreCase(value, dataType.dataType)) {
				return dataType;
			}
		}
		return null;
	}

	private final String dataType;

	private final ValueType valueType;

	private DataType(String dataType, ValueType valueType) {
		this.dataType = dataType;
		this.valueType = valueType;
	}

	public String getDataType() {
		return dataType;
	}

	public ValueType getValueType() {
		return valueType;
	}
}
