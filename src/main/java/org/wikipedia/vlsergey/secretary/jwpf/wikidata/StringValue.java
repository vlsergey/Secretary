package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.function.Function;

import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Text;

public class StringValue extends DataValue {

	protected StringValue(JSONObject jsonObject) {
		super(jsonObject);
	}

	public StringValue(String value) {
		super(new JSONObject());

		jsonObject.put(KEY_TYPE, ValueType.STRING.code);
		jsonObject.put(KEY_VALUE, value);
	}

	public String getValue() {
		return jsonObject.getString(KEY_VALUE);
	}

	@Override
	public int hashCode() {
		return getValue().hashCode();
	}

	@Override
	public Content toWiki(Function<EntityId, String> labelResolver) {
		return new Text(getValue());
	}

}
