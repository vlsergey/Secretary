package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.actions.WbEditEntityAction;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.actions.WbGetEntitiesAction;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.actions.WbRemoveClaimsAction;

public class WikidataBot extends MediaWikiBot {

	private static final Log log = LogFactory.getLog(MediaWikiBot.class);

	@Override
	public boolean isCachedRevisionValid(Revision stored) {
		if (!super.isCachedRevisionValid(stored)) {
			return false;
		}

		if (stored.getPage().getNamespace().intValue() == Namespace.MAIN.id) {
			try {
				Entity apiEntity = new Entity(new JSONObject(stored.getContent()));
				apiEntity.hasClaims(Properties.INSTANCE_OF);
				apiEntity.hasLabel("en");
				apiEntity.hasDescription("en");
				apiEntity.hasSitelink("enwiki");
				return true;
			} catch (Exception exc) {
				log.debug("Invalid cached entity: " + exc, exc);
				return false;
			}
		}

		return true;
	}

	public String queryTokenEdit(EntityId entityId) throws ActionException, ProcessException {
		return queryTokenEdit();
	}

	/**
	 * Create entity in Wikidata
	 */
	public Entity wgCreateEntity(JSONObject data, String summary) {
		enforceWriteLimit();

		String token = queryTokenEdit();
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
	public Entity wgEditEntity(Entity entity, JSONObject data, String summary) {
		enforceWriteLimit();

		String token = queryTokenEdit(entity.getId());
		WbEditEntityAction action = new WbEditEntityAction(isBot());
		action.id = entity.getId();
		action.data = data;
		action.summary = summary;
		action.token = token;
		action.build();
		performAction(action);
		return action.result;
	}

	public Entity wgGetEntity(EntityId entityId, EntityProperty... props) {
		WbGetEntitiesAction action = new WbGetEntitiesAction(isBot());
		action.ids = new String[] { entityId.toString() };
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

	public Entity wgGetEntityBySitelink(String site, String title, EntityProperty... props) {
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
		enforceWriteLimit();

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
