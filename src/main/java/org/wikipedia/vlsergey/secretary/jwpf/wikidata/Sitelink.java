package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class Sitelink extends ApiValue {

	protected Sitelink(JSONObject jsonObject) {
		super(jsonObject);
	}

	public String getSite() {
		return jsonObject.getString("site");
	}

	public String getTitle() {
		return jsonObject.getString("title");
	}

}
