package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;

public class ApiEntity extends ApiValue implements Entity {

	private static final Statement[] CLAIMS_EMTPY = new Statement[0];

	public static final String KEY_CLAIMS = "claims";

	public static final String KEY_ID = "id";

	public static final String KEY_LABELS = "labels";

	public static final String KEY_SITELINKS = "sitelinks";

	public static void putProperty(final JSONObject json, EntityId property, ApiStatement apiStatement) {
		putToNamedMapArray(json, KEY_CLAIMS, property.toString(), apiStatement.jsonObject);
	}

	public ApiEntity(JSONObject jsonObject) {
		super(jsonObject);
	}

	@Override
	public Statement[] getClaims(EntityId property) {
		return getNamedMapArray(KEY_CLAIMS, property.toString(), //
				size -> (size.intValue() == 0 ? CLAIMS_EMTPY : new Statement[size]), //
				obj -> new ApiStatement(obj));
	}

	@Override
	public EntityId getId() {
		return EntityId.parse(jsonObject.getString(KEY_ID));
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
		if (!jsonObject.has(KEY_LABELS)) {
			return false;
		}
		JSONObject labels = jsonObject.getJSONObject(KEY_LABELS);
		return labels.has(langCode);
	}

	@Override
	public boolean hasLabel(String langCode) {
		if (!jsonObject.has(KEY_LABELS)) {
			return false;
		}
		JSONObject labels = jsonObject.getJSONObject(KEY_LABELS);
		return labels.has(langCode);
	}

	@Override
	public boolean hasSitelink(String projectCode) {
		if (!jsonObject.has(KEY_SITELINKS)) {
			return false;
		}
		JSONObject sitelinks = jsonObject.getJSONObject(KEY_SITELINKS);
		return sitelinks.has(projectCode);
	}

}
