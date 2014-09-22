package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class ApiEntity extends ApiValue implements Entity {

	private static final ApiStatement[] CLAIMS_EMTPY = new ApiStatement[0];

	public static final String KEY_CLAIMS = "claims";
	public static final String KEY_DESCRIPTIONS = "descriptions";
	public static final String KEY_ID = "id";
	public static final String KEY_LABELS = "labels";
	public static final String KEY_LASTREVID = "lastrevid";
	public static final String KEY_SITELINKS = "sitelinks";

	public static void putProperty(final JSONObject json, ApiStatement apiStatement) {
		putToNamedMapArray(json, KEY_CLAIMS, apiStatement.getMainSnak().getProperty().toString().toUpperCase(),
				apiStatement.jsonObject);
	}

	public ApiEntity(JSONObject jsonObject) {
		super(jsonObject);
	}

	@Override
	public ApiStatement[] getClaims(EntityId property) {
		return getNamedMapArray(KEY_CLAIMS, property.toString(), //
				size -> (size.intValue() == 0 ? CLAIMS_EMTPY : new ApiStatement[size]), //
				obj -> new ApiStatement(obj));
	}

	@Override
	public Label getDescription(String code) {
		if (!jsonObject.has(KEY_DESCRIPTIONS)) {
			return null;
		}
		JSONObject descriptions = jsonObject.getJSONObject(KEY_DESCRIPTIONS);
		if (!descriptions.has(code)) {
			return null;
		}
		return new Label(descriptions.getJSONObject(code));
	}

	@Override
	public EntityId getId() {
		return EntityId.parse(jsonObject.getString(KEY_ID));
	}

	@Override
	public Label getLabel(String code) {
		if (!jsonObject.has(KEY_LABELS)) {
			return null;
		}
		JSONObject labels = jsonObject.getJSONObject(KEY_LABELS);
		if (!labels.has(code)) {
			return null;
		}
		return new Label(labels.getJSONObject(code));
	}

	public Long getLastRevisionId() {
		return jsonObject.getLong(KEY_LASTREVID);
	}

	@Override
	public Sitelink getSiteLink(String code) {
		if (!jsonObject.has(KEY_SITELINKS)) {
			return null;
		}
		JSONObject sitelinks = jsonObject.getJSONObject(KEY_SITELINKS);
		if (!sitelinks.has(code)) {
			return null;
		}
		return new Sitelink(sitelinks.getJSONObject(code));
	}

	@Override
	public boolean hasClaims(EntityId property) {
		if (!jsonObject.has(KEY_CLAIMS)) {
			return false;
		}
		JSONObject claims = jsonObject.getJSONObject(KEY_CLAIMS);
		return claims.has(property.toString());
	}

	@Override
	public boolean hasDescription(String langCode) {
		if (!jsonObject.has(KEY_DESCRIPTIONS)) {
			return false;
		}
		if (!(jsonObject.get(KEY_DESCRIPTIONS) instanceof JSONObject)) {
			return false;
		}
		return jsonObject.getJSONObject(KEY_DESCRIPTIONS).has(langCode);
	}

	@Override
	public boolean hasLabel(String langCode) {
		if (!jsonObject.has(KEY_LABELS)) {
			return false;
		}
		if (!(jsonObject.get(KEY_LABELS) instanceof JSONObject)) {
			return false;
		}
		return jsonObject.getJSONObject(KEY_LABELS).has(langCode);
	}

	@Override
	public boolean hasSitelink(String projectCode) {
		if (!jsonObject.has(KEY_SITELINKS)) {
			return false;
		}
		JSONObject sitelinks = jsonObject.getJSONObject(KEY_SITELINKS);
		return sitelinks.has(projectCode);
	}

	public void putClaim(ApiStatement apiStatement) {
		putToNamedMapArray(jsonObject, KEY_CLAIMS, apiStatement.getMainSnak().getProperty().toString(),
				apiStatement.jsonObject);
	}

	public void putLabel(Label label) {
		if (!jsonObject.has(KEY_LABELS)) {
			jsonObject.put(KEY_LABELS, new JSONObject());
		}
		jsonObject.getJSONObject(KEY_LABELS).put(label.getLanguage(), label.jsonObject);
	}

	public void setId(EntityId entityId) {
		jsonObject.put(KEY_ID, entityId.toString());
	}

}
