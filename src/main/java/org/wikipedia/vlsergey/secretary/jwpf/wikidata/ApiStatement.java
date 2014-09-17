package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class ApiStatement extends ApiValue implements Statement {

	public static final String KEY_ID = "id";

	public static final String KEY_MAINSNAK = "mainsnak";

	public static final String KEY_QUALIFIERS = "qualifiers";

	public static final String KEY_RANK = "rank";

	public static final String KEY_REFERENCES = "references";

	private static final Snak[] SNAKS_EMTPY = new Snak[0];

	public static ApiStatement newStatement(EntityId property, ApiSnak snak) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.statement);
		statement.setRank(Rank.normal);
		statement.setMainSnak(snak);
		return statement;
	}

	public static ApiStatement newStatement(EntityId property, DataType dataType, DataValue value) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.statement);
		statement.setRank(Rank.normal);
		statement.setMainSnak(ApiSnak.newSnak(property, dataType, value));
		return statement;
	}

	public static ApiStatement newStatement(EntityId property, EntityId entityId) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.statement);
		statement.setRank(Rank.normal);
		statement.setMainSnak(ApiSnak.newSnak(property, entityId));
		return statement;
	}

	public static ApiStatement newStatement(EntityId property, SnakType snakType) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.statement);
		statement.setRank(Rank.normal);
		statement.setMainSnak(ApiSnak.newSnak(property, snakType));
		return statement;
	}

	public static ApiStatement newStatement(EntityId property, String value) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.statement);
		statement.setRank(Rank.normal);
		statement.setMainSnak(ApiSnak.newSnak(property, value));
		return statement;
	}

	public ApiStatement() {
		super(new JSONObject());
	}

	public ApiStatement(final JSONObject jsonObject) {
		super(jsonObject);
	}

	@Override
	public void addQualifier(String propertyCode, ApiSnak qualifier) {
		putToNamedMapArray(jsonObject, KEY_QUALIFIERS, propertyCode, qualifier.jsonObject);
	}

	public void addReference(ApiReference apiReference) {
		if (!jsonObject.has(KEY_REFERENCES)) {
			jsonObject.put(KEY_REFERENCES, new JSONArray());
		}
		jsonObject.getJSONArray(KEY_REFERENCES).put(apiReference.jsonObject);
	}

	@Override
	public String getId() {
		return jsonObject.getString(KEY_ID);
	}

	@Override
	public ApiSnak getMainSnak() {
		return new ApiSnak(jsonObject.getJSONObject(KEY_MAINSNAK));
	}

	@Override
	public Snak[] getQualifiers() {
		if (!jsonObject.has(KEY_QUALIFIERS)) {
			return new ApiSnak[0];
		}

		List<ApiSnak> result = new ArrayList<>();
		final JSONObject map = jsonObject.getJSONObject(KEY_QUALIFIERS);
		for (Object key : map.keySet()) {
			JSONArray jsonArray = map.getJSONArray(key.toString());
			for (int i = 0; i < jsonArray.length(); i++) {
				result.add(new ApiSnak(jsonArray.getJSONObject(i)));
			}
		}
		return result.toArray(new ApiSnak[result.size()]);
	}

	@Override
	public Snak[] getQualifiers(EntityId property) {
		return getNamedMapArray(KEY_QUALIFIERS, property.toString().toUpperCase(), //
				size -> (size.intValue() == 0 ? SNAKS_EMTPY : new Snak[size]), //
				obj -> new ApiSnak(obj));
	}

	@Override
	public Rank getRank() {
		return Rank.valueOf(jsonObject.getString(KEY_RANK));
	}

	@Override
	public ApiReference[] getReferences() {
		if (!jsonObject.has(KEY_REFERENCES)) {
			return new ApiReference[0];
		}
		List<ApiReference> result = new ArrayList<>();
		final JSONArray jsonArray = jsonObject.getJSONArray(KEY_REFERENCES);
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject obj = jsonArray.getJSONObject(i);
			result.add(new ApiReference(obj));
		}
		return result.toArray(new ApiReference[result.size()]);
	}

	@Override
	public boolean hasMainSnak() {
		return jsonObject.has(KEY_MAINSNAK);
	}

	@Override
	public void setMainSnak(ApiSnak mainSnak) {
		jsonObject.put(KEY_MAINSNAK, mainSnak.jsonObject);
	}

	@Override
	public void setRank(Rank rank) {
		jsonObject.put(KEY_RANK, rank.name());
	}
}
