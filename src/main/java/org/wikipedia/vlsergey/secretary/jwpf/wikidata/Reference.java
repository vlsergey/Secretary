package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class Reference extends Value {

	private static final String KEY_HASH = "hash";
	private static final String KEY_SNAKS = "snaks";

	private static final Snak[] SNAKS_EMPTY = new Snak[0];

	public Reference() {
		super(new JSONObject());
	}

	public Reference(JSONObject jsonObject) {
		super(jsonObject);
	}

	public void addSnak(Snak snak) {
		putToNamedMapArray(jsonObject, KEY_SNAKS, snak.getProperty().toString().toUpperCase(), snak.jsonObject);
	}

	public String getHash() {
		return jsonObject.getString(KEY_HASH);
	}

	public Snak[] getSnaks() {
		if (!jsonObject.has(KEY_SNAKS)) {
			return SNAKS_EMPTY;
		}
		JSONObject snaks = jsonObject.getJSONObject(KEY_SNAKS);
		JSONArray names = snaks.names();
		if (names == null) {
			return SNAKS_EMPTY;
		}
		List<Snak> result = new ArrayList<>();
		for (int i = 0; i < names.length(); i++) {
			JSONArray array = snaks.getJSONArray(names.getString(i));
			for (int k = 0; k < array.length(); k++) {
				result.add(new Snak(array.getJSONObject(k)));
			}
		}
		return result.toArray(new Snak[result.size()]);
	}

	public List<Snak> getSnaks(EntityId property) {
		return getNamedMapList(KEY_SNAKS, property.toString().toUpperCase(), obj -> new Snak(obj));
	}

	public void setHash(String hash) {
		jsonObject.put(KEY_HASH, hash);
	}

}
