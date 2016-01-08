package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class Value {

	public static final String KEY_TYPE = "type";

	protected static <T> T getValueFromValuesNamedMap(JSONObject jsonObject, String mapName, String key,
			Function<JSONObject, T> itemBuilder) {
		if (!jsonObject.has(mapName)) {
			return null;
		}

		if (isEmptyArray(jsonObject, mapName)) {
			return null;
		}

		final JSONObject map = jsonObject.getJSONObject(mapName);
		if (!map.has(key)) {
			return null;
		}

		JSONObject itemObject = map.getJSONObject(key);
		return itemBuilder.apply(itemObject);
	}

	protected static <T> List<T> getValuesFromArraysNamedMap(JSONObject jsonObject, String mapName,
			Function<JSONObject, T> itemBuilder) {
		if (!jsonObject.has(mapName)) {
			return Collections.emptyList();
		}

		if (isEmptyArray(jsonObject, mapName)) {
			return Collections.emptyList();
		}

		List<T> result = new ArrayList<>();
		final JSONObject map = jsonObject.getJSONObject(mapName);
		for (Object key : map.keySet()) {
			JSONArray jsonArray = map.getJSONArray(key.toString());
			for (int i = 0; i < jsonArray.length(); i++) {
				result.add(itemBuilder.apply(jsonArray.getJSONObject(i)));
			}
		}
		return result;
	}

	protected static <T> List<T> getValuesFromValuesNamedMap(JSONObject jsonObject, String mapName,
			Function<JSONObject, T> itemBuilder) {
		if (!jsonObject.has(mapName)) {
			return Collections.emptyList();
		}

		if (isEmptyArray(jsonObject, mapName)) {
			return Collections.emptyList();
		}

		List<T> result = new ArrayList<>();
		final JSONObject map = jsonObject.getJSONObject(mapName);
		for (Object key : map.keySet()) {
			JSONObject itemObject = map.getJSONObject(key.toString());
			result.add(itemBuilder.apply(itemObject));
		}
		return result;
	}

	protected static boolean hasInNamedMap(JSONObject jsonObject, String mapName, String key) {
		if (!jsonObject.has(mapName)) {
			return false;
		}
		if (isEmptyArray(jsonObject, mapName)) {
			return false;
		}
		JSONObject map = jsonObject.getJSONObject(mapName);
		return map.has(key);
	}

	protected static boolean isEmptyArray(JSONObject jsonObject, String key) {
		// see https://bugzilla.wikimedia.org/show_bug.cgi?id=71458
		if (jsonObject.get(key) instanceof JSONArray) {
			if (jsonObject.getJSONArray(key).length() == 0) {
				return true;
			} else {
				throw new RuntimeException("Outdated format of entity");
			}
		}
		return false;
	}

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

	protected Value(JSONObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof Value
				&& StringUtils.equals(this.jsonObject.toString(), ((Value) obj).jsonObject.toString());
	}

	public JSONObject getJsonObject() {
		return jsonObject;
	}

	protected <T> List<T> getNamedMapList(String mapName, String key, Function<JSONObject, T> itemBuilder) {
		if (!jsonObject.has(mapName)) {
			return Collections.emptyList();
		}
		if (isEmptyArray(jsonObject, mapName)) {
			return Collections.emptyList();
		}
		final JSONObject map = jsonObject.getJSONObject(mapName);
		if (!map.has(key)) {
			return Collections.emptyList();
		}
		JSONArray jsonArray = map.getJSONArray(key);
		List<T> result = new ArrayList<>(jsonArray.length());
		for (int i = 0; i < jsonArray.length(); i++) {
			result.add(itemBuilder.apply(jsonArray.getJSONObject(i)));
		}
		return result;
	}

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

	public void setType(ValueType type) {
		jsonObject.put(KEY_TYPE, type.code);
	}

	@Override
	public String toString() {
		return jsonObject.toString();
	}

}
