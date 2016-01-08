package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

public class Statement extends Value {

	public static final String KEY_ID = "id";

	public static final String KEY_MAINSNAK = "mainsnak";

	public static final String KEY_QUALIFIERS = "qualifiers";

	public static final String KEY_RANK = "rank";

	public static final String KEY_REFERENCES = "references";

	private static final Snak[] SNAKS_EMTPY = new Snak[0];

	public static Statement newStatement(EntityId property, DataType dataType, DataValue value) {
		Statement statement = new Statement();
		statement.setType(ValueType.STATEMENT);
		statement.setRank(Rank.normal);
		statement.setMainSnak(Snak.newSnak(property, dataType, value));
		return statement;
	}

	public static Statement newStatement(EntityId property, EntityId entityId) {
		Statement statement = new Statement();
		statement.setType(ValueType.STATEMENT);
		statement.setRank(Rank.normal);
		statement.setMainSnak(Snak.newSnak(property, entityId));
		return statement;
	}

	public static Statement newStatement(EntityId property, Snak snak) {
		Statement statement = new Statement();
		statement.setType(ValueType.STATEMENT);
		statement.setRank(Rank.normal);
		statement.setMainSnak(snak);
		return statement;
	}

	public static Statement newStatement(EntityId property, SnakType snakType) {
		Statement statement = new Statement();
		statement.setType(ValueType.STATEMENT);
		statement.setRank(Rank.normal);
		statement.setMainSnak(Snak.newSnak(property, snakType));
		return statement;
	}

	public static Statement newStatement(EntityId property, String value) {
		Statement statement = new Statement();
		statement.setType(ValueType.STATEMENT);
		statement.setRank(Rank.normal);
		statement.setMainSnak(Snak.newSnak(property, value));
		return statement;
	}

	public Statement() {
		super(new JSONObject());
	}

	public Statement(final JSONObject jsonObject) {
		super(jsonObject);
	}

	public void addQualifier(Snak qualifier) {
		putToNamedMapArray(jsonObject, KEY_QUALIFIERS, qualifier.getProperty().toString(), qualifier.jsonObject);
	}

	public void addReference(Reference apiReference) {
		if (!jsonObject.has(KEY_REFERENCES)) {
			jsonObject.put(KEY_REFERENCES, new JSONArray());
		}
		jsonObject.getJSONArray(KEY_REFERENCES).put(apiReference.jsonObject);
	}

	public String getId() {
		return jsonObject.getString(KEY_ID);
	}

	public Snak getMainSnak() {
		return new Snak(jsonObject.getJSONObject(KEY_MAINSNAK));
	}

	public Snak[] getQualifiers() {
		if (!jsonObject.has(KEY_QUALIFIERS)) {
			return new Snak[0];
		}

		List<Snak> result = new ArrayList<>();
		final JSONObject map = jsonObject.getJSONObject(KEY_QUALIFIERS);
		for (Object key : map.keySet()) {
			JSONArray jsonArray = map.getJSONArray(key.toString());
			for (int i = 0; i < jsonArray.length(); i++) {
				result.add(new Snak(jsonArray.getJSONObject(i)));
			}
		}
		return result.toArray(new Snak[result.size()]);
	}

	public List<Snak> getQualifiers(EntityId property) {
		return getNamedMapList(KEY_QUALIFIERS, property.toString().toUpperCase(), obj -> new Snak(obj));
	}

	public Rank getRank() {
		return Rank.valueOf(jsonObject.getString(KEY_RANK));
	}

	public Reference[] getReferences() {
		if (!jsonObject.has(KEY_REFERENCES)) {
			return new Reference[0];
		}
		List<Reference> result = new ArrayList<>();
		final JSONArray jsonArray = jsonObject.getJSONArray(KEY_REFERENCES);
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject obj = jsonArray.getJSONObject(i);
			result.add(new Reference(obj));
		}
		return result.toArray(new Reference[result.size()]);
	}

	public StringValue getStringValue() {
		return getMainSnak().getStringValue();
	}

	public ValueType getValueType() {
		return getMainSnak().getValueType();
	}

	public boolean hasMainSnak() {
		return jsonObject.has(KEY_MAINSNAK);
	}

	public boolean hasRealReferences() {
		// has any references except ones with "obtained from"
		for (Reference reference : getReferences()) {
			for (Snak snak : reference.getSnaks()) {
				if (Properties.IMPORTED_FROM.equals(snak.getProperty())
						|| Properties.DATE_RETRIEVED.equals(snak.getProperty())) {
					continue;
				}
				return true;
			}
		}
		return false;
	}

	public boolean hasValue() {
		return hasMainSnak() && getMainSnak().getSnakType() == SnakType.value;
	}

	/**
	 * @return <tt>true</tt> if and only if the statement has single reference
	 *         and it is "imported from" specified place
	 */
	public boolean isImportedFrom(EntityId entityId) {
		Reference[] references = getReferences();
		if (references.length != 1) {
			return false;
		}
		Snak[] snaks = references[0].getSnaks();
		WikibaseEntityIdValue toCompare = new WikibaseEntityIdValue(entityId);

		switch (snaks.length) {
		case 1:
			return Properties.IMPORTED_FROM.equals(snaks[0].getProperty()) && snaks[0].getSnakType() == SnakType.value
					&& toCompare.equals(snaks[0].getWikibaseEntityIdValue());
		case 2:
			return Properties.IMPORTED_FROM.equals(snaks[0].getProperty()) && snaks[0].getSnakType() == SnakType.value
					&& toCompare.equals(snaks[0].getWikibaseEntityIdValue())
					&& Properties.DATE_RETRIEVED.equals(snaks[1].getProperty())

					|| Properties.IMPORTED_FROM.equals(snaks[1].getProperty())
					&& snaks[1].getSnakType() == SnakType.value
					&& toCompare.equals(snaks[1].getWikibaseEntityIdValue())
					&& Properties.DATE_RETRIEVED.equals(snaks[0].getProperty());
		}
		return false;
	}

	public boolean isWikibaseEntityIdValue(EntityId entityId) {
		return hasValue() && getMainSnak().isWikibaseEntityIdValue(entityId);
	}

	public void removeQualifier(Snak qualifier) {
		final String hash = qualifier.getHash();
		if (StringUtils.isBlank(hash)) {
			throw new IllegalArgumentException("Hash is not specified");
		}

		removeFromMapArray(KEY_QUALIFIERS, qualifier.getProperty().toString(),
				x -> StringUtils.equalsIgnoreCase(hash, x.getString("hash")));
	}

	public void setId(String id) {
		jsonObject.put(KEY_ID, id);
	}

	public void setMainSnak(Snak mainSnak) {
		jsonObject.put(KEY_MAINSNAK, mainSnak.jsonObject);
	}

	public void setRank(Rank rank) {
		jsonObject.put(KEY_RANK, rank.name());
	}
}
