package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class ApiReference extends ApiValue implements Reference {

	private static final String KEY_HASH = "hash";
	private static final String KEY_SNAKS = "snaks";

	private static final ApiSnak[] SNAKS_EMPTY = new ApiSnak[0];

	public ApiReference() {
		super(new JSONObject());
	}

	public ApiReference(JSONObject jsonObject) {
		super(jsonObject);
	}

	public void addSnak(ApiSnak snak) {
		putToNamedMapArray(jsonObject, KEY_SNAKS, snak.getProperty().toString().toUpperCase(), snak.jsonObject);
	}

	@Override
	public String getHash() {
		return jsonObject.getString(KEY_HASH);
	}

	@Override
	public ApiSnak[] getSnaks() {
		if (!jsonObject.has(KEY_SNAKS)) {
			return SNAKS_EMPTY;
		}
		JSONObject snaks = jsonObject.getJSONObject(KEY_SNAKS);
		JSONArray names = snaks.names();
		if (names == null) {
			return SNAKS_EMPTY;
		}
		List<ApiSnak> result = new ArrayList<>();
		for (int i = 0; i < names.length(); i++) {
			JSONArray array = snaks.getJSONArray(names.getString(i));
			for (int k = 0; k < array.length(); k++) {
				result.add(new ApiSnak(array.getJSONObject(k)));
			}
		}
		return result.toArray(new ApiSnak[result.size()]);
	}

	@Override
	public ApiSnak[] getSnaks(EntityId property) {
		return getNamedMapArray(KEY_SNAKS, property.toString().toUpperCase(), //
				size -> (size.intValue() == 0 ? SNAKS_EMPTY : new ApiSnak[size]), //
				obj -> new ApiSnak(obj));
	}

	@Override
	public void setHash(String hash) {
		jsonObject.put(KEY_HASH, hash);
	}

}
