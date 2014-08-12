package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class NativeEntity extends NativeValue implements Entity {

	private static final String KEY_CLAIMS = "claims";
	private static final String KEY_ENTITY = "entity";

	private final JSONObject jsonObject;

	public NativeEntity(JSONObject jsonObject) {
		this.jsonObject = jsonObject;
	}

	@Override
	public Statement[] getClaims(EntityId property) {
		if (!jsonObject.has(KEY_CLAIMS)) {
			return new Statement[0];
		}

		List<NativeStatement> statements = new ArrayList<>();
		JSONArray claims = jsonObject.getJSONArray(KEY_CLAIMS);
		for (int i = 0; i < claims.length(); i++) {
			JSONObject claimObject = claims.getJSONObject(i);
			NativeStatement statement = new NativeStatement(claimObject);
			Snak mainSnak = statement.getMainSnak();
			if (property.equals(mainSnak.getProperty())) {
				statements.add(statement);
			}
		}

		return statements.toArray(new Statement[statements.size()]);
	}

	@Override
	public EntityId getId() {
		JSONArray entity = jsonObject.getJSONArray(KEY_ENTITY);
		EntityType entityType = EntityType.valueOf(entity.getString(0));
		long id = entity.getLong(1);
		return new EntityId(entityType, id);
	}

	@Override
	public Sitelink getSiteLink(String code) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public ValueType getType() {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public boolean hasClaims(EntityId property) {
		JSONArray claims = jsonObject.getJSONArray(KEY_CLAIMS);
		for (int i = 0; i < claims.length(); i++) {
			JSONObject claimObject = claims.getJSONObject(i);
			NativeStatement statement = new NativeStatement(claimObject);
			Snak mainSnak = statement.getMainSnak();
			if (property.equals(mainSnak.getProperty())) {
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean hasDescription(String langCode) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public boolean hasLabel(String langCode) {
		throw new UnsupportedOperationException("NYI");
	}

	@Override
	public boolean hasSitelink(String projectCode) {
		throw new UnsupportedOperationException("NYI");
	}
}
