package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class Label extends ApiValue {

	private static final String KEY_LANGUAGE = "language";
	private static final String KEY_VALUE = "value";

	public Label(JSONObject jsonObject) {
		super(jsonObject);
	}

	public Label(String language, String value) {
		super(new JSONObject());
		setLanguage(language);
		setValue(value);
	}

	public String getLanguage() {
		return jsonObject.getString(KEY_LANGUAGE);
	}

	public String getValue() {
		return jsonObject.getString(KEY_VALUE);
	}

	public void setLanguage(String language) {
		jsonObject.put(KEY_LANGUAGE, language);
	}

	public void setValue(String value) {
		jsonObject.put(KEY_VALUE, value);
	}

}
