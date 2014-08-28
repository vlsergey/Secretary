package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class NativeStatement extends NativeValue implements Statement {

	static String KEY_MAINSNAK = "m";
	static String KEY_QUALIFIERS = "q";

	private final JSONObject jsonObject;

	public NativeStatement(final JSONObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	@Override
	public void addQualifier(String propertyCode, ApiSnak qualifier) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public Snak getMainSnak() {
		return new NativeSnak(jsonObject.getJSONArray(KEY_MAINSNAK));
	}

	@Override
	public Snak[] getQualifiers(EntityId property) {
		if (!jsonObject.has(KEY_QUALIFIERS)) {
			return new Snak[0];
		}

		List<NativeSnak> qualifiers = new ArrayList<>();
		JSONArray snaks = jsonObject.getJSONArray(KEY_QUALIFIERS);
		for (int i = 0; i < snaks.length(); i++) {
			JSONArray qualifierObject = snaks.getJSONArray(i);
			NativeSnak snak = new NativeSnak(qualifierObject);
			if (property.equals(snak.getProperty())) {
				qualifiers.add(snak);
			}
		}

		return qualifiers.toArray(new Snak[qualifiers.size()]);
	}

	@Override
	public Rank getRank() {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public ValueType getType() {
		return ValueType.statement;
	}

	@Override
	public boolean hasMainSnak() {
		return jsonObject.has(KEY_MAINSNAK);
	}

	@Override
	public void setMainSnak(ApiSnak mainSnak) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public void setRank(Rank rank) {
		throw new UnsupportedOperationException("NYI");
	}
}
