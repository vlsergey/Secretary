package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.actions.WbEditEntityAction;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.actions.WbGetEntitiesAction;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.actions.WbRemoveClaimsAction;

public class WikidataBot extends MediaWikiBot {

	public String queryTokenEdit(EntityId entityId) throws ActionException, ProcessException {
		return queryTokenEdit(entityId.toString());
	}

	/**
	 * Create entity in Wikidata
	 */
	public ApiEntity wgCreateEntity(JSONObject data) {
		String token = queryTokenEdit("Q1");
		WbEditEntityAction action = new WbEditEntityAction(isBot());
		action.new_ = "item";
		action.data = data;
		action.token = token;
		action.build();
		performAction(action);
		return action.result;
	}

	/**
	 * Edit entity in Wikidata
	 */
	public ApiEntity wgEditEntity(Entity apiEntity, JSONObject data, String summary) {
		String token = queryTokenEdit(apiEntity.getId());
		WbEditEntityAction action = new WbEditEntityAction(isBot());
		action.id = apiEntity.getId();
		action.data = data;
		action.summary = summary;
		action.token = token;
		action.build();
		performAction(action);
		return action.result;
	}

	public ApiEntity wgGetEntity(String entityId, EntityProperty... props) {
		WbGetEntitiesAction action = new WbGetEntitiesAction(isBot());
		action.ids = new String[] { entityId };
		action.normalize = Boolean.TRUE;
		action.props = props;
		action.build();
		performAction(action);

		if (action.result.size() == 0) {
			return null;
		}
		if (action.result.size() > 1) {
			throw new RuntimeException("Too many entities returned by action [" + action + "]: " + action.result);
		}
		return action.result.values().iterator().next();
	}

	public ApiEntity wgGetEntityBySitelink(String site, String title, EntityProperty... props) {
		WbGetEntitiesAction action = new WbGetEntitiesAction(isBot());
		action.normalize = Boolean.TRUE;
		action.sites = new String[] { site };
		action.titles = new String[] { title };
		action.props = props;
		action.build();
		performAction(action);

		if (action.result.size() == 0) {
			return null;
		}
		if (action.result.size() > 1) {
			throw new RuntimeException("Too many entities returned by action [" + action + "]: " + action.result);
		}
		return action.result.values().iterator().next();
	}

	/**
	 * Edit entity in Wikidata
	 */
	public void wgRemoveClaims(Entity entity, String[] claims, String summary) {
		String token = queryTokenEdit(entity.getId());
		WbRemoveClaimsAction action = new WbRemoveClaimsAction(isBot());
		action.claim = claims;
		action.summary = summary;
		action.token = token;
		action.build();
		performAction(action);
		return;
	}

}
