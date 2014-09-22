package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.function.Function;
import java.util.function.Predicate;

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
		return ValueType.byCode(jsonObject.getString(KEY_TYPE));
	}

	protected JSONObject removeFromMapArray(String mapName, String key, Predicate<JSONObject> predicate) {
		if (!jsonObject.has(mapName)) {
			throw new IllegalArgumentException("no map '" + mapName + "'");
		}
		final JSONObject map = jsonObject.getJSONObject(mapName);
		if (!map.has(key)) {
			throw new IllegalArgumentException("no item '" + key + "' in map '" + mapName + "'");
		}
		JSONArray jsonArray = map.getJSONArray(key);
		for (int i = jsonArray.length() - 1; i >= 0; i--) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			if (predicate.test(jsonObject)) {
				jsonArray.remove(i);
				return jsonObject;
			}
		}
		throw new IllegalArgumentException("Item not found");
	}

	@Override
	public void setType(ValueType type) {
		jsonObject.put(KEY_TYPE, type.code);
	}

	@Override
	public String toString() {
		return jsonObject.toString();
	}

}
