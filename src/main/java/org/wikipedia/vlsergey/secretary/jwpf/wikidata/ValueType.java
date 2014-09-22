package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.apache.commons.lang.StringUtils;

public enum ValueType {

	STATEMENT("statement"),

	STRING("string"),

	TIME("time"),

	WIKIBASE_ENTITYID("wikibase-entityid"),

	;

	public static ValueType byCode(String code) {
		for (ValueType valueType : values()) {
			if (StringUtils.equalsIgnoreCase(code, valueType.code)) {
				return valueType;
			}
		}
		return ValueType.valueOf(code);
	}

	public final String code;

	private ValueType(final String code) {
		this.code = code;
	}

}
