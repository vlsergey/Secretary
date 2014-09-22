package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class ApiStatement extends ApiValue implements Statement {

	public static final String KEY_ID = "id";

	public static final String KEY_MAINSNAK = "mainsnak";

	public static final String KEY_QUALIFIERS = "qualifiers";

	public static final String KEY_RANK = "rank";

	public static final String KEY_REFERENCES = "references";

	private static final ApiSnak[] SNAKS_EMTPY = new ApiSnak[0];

	public static ApiStatement newStatement(EntityId property, ApiSnak snak) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.STATEMENT);
		statement.setRank(Rank.normal);
		statement.setMainSnak(snak);
		return statement;
	}

	public static ApiStatement newStatement(EntityId property, DataType dataType, DataValue value) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.STATEMENT);
		statement.setRank(Rank.normal);
		statement.setMainSnak(ApiSnak.newSnak(property, dataType, value));
		return statement;
	}

	public static ApiStatement newStatement(EntityId property, EntityId entityId) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.STATEMENT);
		statement.setRank(Rank.normal);
		statement.setMainSnak(ApiSnak.newSnak(property, entityId));
		return statement;
	}

	public static ApiStatement newStatement(EntityId property, SnakType snakType) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.STATEMENT);
		statement.setRank(Rank.normal);
		statement.setMainSnak(ApiSnak.newSnak(property, snakType));
		return statement;
	}

	public static ApiStatement newStatement(EntityId property, String value) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.STATEMENT);
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
	public void addQualifier(ApiSnak qualifier) {
		putToNamedMapArray(jsonObject, KEY_QUALIFIERS, qualifier.getProperty().toString(), qualifier.jsonObject);
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
	public ApiSnak[] getQualifiers() {
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
	public ApiSnak[] getQualifiers(EntityId property) {
		return getNamedMapArray(KEY_QUALIFIERS, property.toString().toUpperCase(), //
				size -> (size.intValue() == 0 ? SNAKS_EMTPY : new ApiSnak[size]), //
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

	public void removeQualifier(ApiSnak qualifier) {
		final String hash = qualifier.getHash();
		if (StringUtils.isBlank(hash)) {
			throw new IllegalArgumentException("Hash is not specified");
		}

		removeFromMapArray(KEY_QUALIFIERS, qualifier.getProperty().toString(),
				x -> StringUtils.equalsIgnoreCase(hash, x.getString("hash")));
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
