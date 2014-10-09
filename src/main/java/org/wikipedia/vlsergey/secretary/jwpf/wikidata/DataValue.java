package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.Locale;
import java.util.function.Function;

import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Text;

public class DataValue extends Value {

	public static final String KEY_TYPE = "type";
	public static final String KEY_VALUE = "value";

	protected DataValue(JSONObject jsonObject) {
		super(jsonObject);
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DataValue && ((DataValue) obj).jsonObject.toString().equals(jsonObject.toString());
	}

	public ValueType getValueType() {
		return ValueType.byCode(jsonObject.getString(KEY_TYPE));
	}

	@Override
	public String toString() {
		return toWiki(Locale.getDefault(), x -> x.toString()).toWiki(true);
	}

	public Content toWiki(Locale locale, Function<EntityId, String> labelResolver) {
		return new Text(jsonObject.toString());
	}
}
