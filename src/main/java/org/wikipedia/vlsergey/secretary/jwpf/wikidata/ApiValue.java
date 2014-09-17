package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class ApiValue implements Value {

	public static final String KEY_TYPE = "type";

	protected static void putToNamedMapArray(JSONObject json, String mapName, String key, JSONObject jsonObject) {
		final JSONObject map;
		if (!json.has(mapName)) {
			map = new JSONObject();
			json.put(mapName, map);
		} else {
			map = json.getJSONObject(mapName);
		}
		if (!map.has(key)) {
			map.put(key, new JSONArray());
		}
		map.getJSONArray(key).put(jsonObject);
	}

	protected final JSONObject jsonObject;

	protected ApiValue(JSONObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof ApiValue
				&& StringUtils.equals(this.jsonObject.toString(), ((ApiValue) obj).jsonObject.toString());
	}

	public JSONObject getJsonObject() {
		return jsonObject;
	}

	protected <T> T[] getNamedMapArray(String mapName, String key, Function<Integer, T[]> arrayBuilder,
			Function<JSONObject, T> itemBuilder) {
		if (!jsonObject.has(mapName)) {
			return arrayBuilder.apply(0);
		}
		final JSONObject map = jsonObject.getJSONObject(mapName);
		if (!map.has(key)) {
			return arrayBuilder.apply(0);
		}
		JSONArray jsonArray = map.getJSONArray(key);
		T[] result = arrayBuilder.apply(jsonArray.length());
		for (int i = 0; i < jsonArray.length(); i++) {
			result[i] = itemBuilder.apply(jsonArray.getJSONObject(i));
		}
		return result;
	}

	@Override
	public ValueType getType() {
		return ValueType.valueOf(jsonObject.getString(KEY_TYPE));
	}

	@Override
	public void setType(ValueType type) {
		jsonObject.put(KEY_TYPE, type.name());
	}

	@Override
	public String toString() {
		return jsonObject.toString();
	}

}
