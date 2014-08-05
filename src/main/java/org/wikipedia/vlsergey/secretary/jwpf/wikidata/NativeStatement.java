package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class NativeStatement extends NativeValue implements Statement {

	static String KEY_MAINSNAK = "m";

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
	public Snak[] getQualifiers(String propertyCode) {
		throw new UnsupportedOperationException("NYI");
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
	public boolean isWikibaseEntityIdValue(String entityId) {
		throw new UnsupportedOperationException("NYI");
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
