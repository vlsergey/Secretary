package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.util.List;

import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

public class Entity extends Value implements Comparable<Entity> {

	public static final String KEY_CLAIMS = "claims";
	public static final String KEY_DESCRIPTIONS = "descriptions";
	public static final String KEY_ID = "id";
	public static final String KEY_LABELS = "labels";
	public static final String KEY_LASTREVID = "lastrevid";
	public static final String KEY_SITELINKS = "sitelinks";

	public static void putProperty(final JSONObject json, Statement apiStatement) {
		putToNamedMapArray(json, KEY_CLAIMS, apiStatement.getMainSnak().getProperty().toString().toUpperCase(),
				apiStatement.jsonObject);
	}

	public Entity(JSONObject jsonObject) {
		super(jsonObject);
	}

	public Entity(Revision revision) {
		super(new JSONObject(revision.getContent()));
	}

	@Override
	public int compareTo(Entity o) {
		return this.getId().compareTo(o.getId());
	}

	public List<Statement> getClaims(EntityId property) {
		return getNamedMapList(KEY_CLAIMS, property.toString(), obj -> new Statement(obj));
	}

	public Label getDescription(String code) {
		return getValueFromValuesNamedMap(jsonObject, KEY_DESCRIPTIONS, code, x -> new Label(x));
	}

	public EntityId getId() {
		return EntityId.parse(jsonObject.getString(KEY_ID));
	}

	public Label getLabel(String code) {
		return getValueFromValuesNamedMap(jsonObject, KEY_LABELS, code, x -> new Label(x));
	}

	public String getLabelValue(String code) {
		Label label = getLabel(code);
		return label == null ? null : label.getValue();
	}

	public Long getLastRevisionId() {
		return jsonObject.getLong(KEY_LASTREVID);
	}

	public Sitelink getSiteLink(String code) {
		return getValueFromValuesNamedMap(jsonObject, KEY_SITELINKS, code, x -> new Sitelink(x));
	}

	public List<Sitelink> getSitelinks() {
		return getValuesFromValuesNamedMap(jsonObject, KEY_SITELINKS, x -> new Sitelink(x));
	}

	public String getSiteLinkTitle(String code) {
		Sitelink sitelink = getSiteLink(code);
		return sitelink == null ? null : sitelink.getTitle();
	}

	public boolean hasClaims(EntityId property) {
		return hasInNamedMap(jsonObject, KEY_CLAIMS, property.toString());
	}

	public boolean hasDescription(String langCode) {
		return hasInNamedMap(jsonObject, KEY_DESCRIPTIONS, langCode);
	}

	public boolean hasLabel(String langCode) {
		return hasInNamedMap(jsonObject, KEY_LABELS, langCode);
	}

	public boolean hasSitelink(String projectCode) {
		return hasInNamedMap(jsonObject, KEY_SITELINKS, projectCode);
	}

	public void putClaim(Statement apiStatement) {
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
