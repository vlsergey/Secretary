package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Text;

public abstract class DataValue extends ApiValue {

	public static final String KEY_VALUE = "value";

	protected DataValue(JSONObject jsonObject) {
		super(jsonObject);
	}

	@Override
	public String toString() {
		return toWiki().toWiki(true);
	}

	public Content toWiki() {
		return new Text(jsonObject.toString());
	}

}
