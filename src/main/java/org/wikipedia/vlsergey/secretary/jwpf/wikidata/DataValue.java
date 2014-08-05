package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public abstract class DataValue extends ApiValue {

	public static final String KEY_TYPE = "type";

	public static final String KEY_VALUE = "value";

	protected DataValue(JSONObject jsonObject) {
		super(jsonObject);
	}

}
