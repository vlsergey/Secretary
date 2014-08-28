package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class Label extends ApiValue {

	protected Label(JSONObject jsonObject) {
		super(jsonObject);
	}

	public String getLanguage() {
		return jsonObject.getString("language");
	}

	public String getValue() {
		return jsonObject.getString("value");
	}

}
