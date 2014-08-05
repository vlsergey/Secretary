package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class StringValue extends DataValue {

	public static final String DATATYPE = "string";

	protected StringValue(JSONObject jsonObject) {
		super(jsonObject);
	}

	public StringValue(String value) {
		super(new JSONObject());

		jsonObject.put(KEY_TYPE, DATATYPE);
		jsonObject.put(KEY_VALUE, value);
	}

	public String getValue() {
		return jsonObject.getString(KEY_VALUE);
	}

}
