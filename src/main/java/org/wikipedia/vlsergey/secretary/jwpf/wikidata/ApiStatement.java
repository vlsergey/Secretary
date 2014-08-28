package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class ApiStatement extends ApiValue implements Statement {

	public static final String KEY_ID = "id";

	public static final String KEY_MAINSNAK = "mainsnak";

	public static final String KEY_QUALIFIERS = "qualifiers";

	public static final String KEY_RANK = "rank";

	private static final Snak[] SNAKS_EMTPY = new Snak[0];

	public static ApiStatement newStatement(EntityId property, DataType dataType, DataValue value) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.statement);
		statement.setRank(Rank.normal);
		statement.setMainSnak(ApiSnak.newSnak(property, dataType, value));
		return statement;
	}

	public static ApiStatement newStatement(EntityId property, SnakType snakType) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.statement);
		statement.setRank(Rank.normal);
		statement.setMainSnak(ApiSnak.newSnak(property, snakType));
		return statement;
	}

	public static ApiStatement newStringValueStatement(EntityId property, String value) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.statement);
		statement.setRank(Rank.normal);
		statement.setMainSnak(ApiSnak.newStringValueSnak(property, value));
		return statement;
	}

	public static ApiStatement newWikibaseEntityIdValueStatement(EntityId property, EntityId entityId) {
		ApiStatement statement = new ApiStatement();
		statement.setType(ValueType.statement);
		statement.setRank(Rank.normal);
		statement.setMainSnak(ApiSnak.newWikibaseEntityIdValueSnak(property, entityId));
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

	@Override
	public String getId() {
		return jsonObject.getString(KEY_ID);
	}

	@Override
	public Snak getMainSnak() {
		return new ApiSnak(jsonObject.getJSONObject(KEY_MAINSNAK));
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
