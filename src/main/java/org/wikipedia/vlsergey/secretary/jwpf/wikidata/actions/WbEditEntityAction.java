package org.wikipedia.vlsergey.secretary.jwpf.wikidata.actions;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.actions.AbstractApiAction;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;

/**
 * API module to create a single new Wikibase entity and modify it with
 * serialised information.
 */
public class WbEditEntityAction extends AbstractApiAction {

	/**
	 * The numeric identifier for the revision to base the modification on. This
	 * is used for detecting conflicts during save.
	 */
	public String baserevid;

	/**
	 * Mark this edit as bot This URL flag will only be respected if the user
	 * belongs to the group "bot".
	 */
	public Boolean bot;

	/**
	 * If set, the complete entity is emptied before proceeding. The entity will
	 * not be saved before it is filled with the "data", possibly with parts
	 * excluded.
	 */
	public Boolean clear;

	/**
	 * The serialized object that is used as the data source. A newly created
	 * entity will be assigned an 'id'.
	 */
	public JSONObject data;

	/**
	 * The identifier for the entity, including the prefix.
	 */
	public EntityId id;

	/**
	 * If set, a new entity will be created. Set this to the type of the entity
	 * you want to create - currently 'item'|'property'. It is not allowed to
	 * have this set when 'id' is also set.
	 */
	public String new_;

	public Entity result;

	/**
	 * An identifier for the site on which the page resides. Use together with
	 * {@link #title} to make a complete sitelink.
	 */
	public String site;

	/**
	 * Summary for the edit. Will be prepended by an automatically generated
	 * comment. The length limit of the autocomment together with the summary is
	 * 260 characters. Be aware that everything above that limit will be cut
	 * off.
	 */
	public String summary;

	/**
	 * Title of the page to associate. Use together with {@link #site} to make a
	 * complete sitelink.
	 */
	public String title;

	/**
	 * A "edittoken" token previously obtained through the token module (
	 * {@link MediaWikiBot#queryTokenEdit(String)} )
	 */
	public String token;

	public WbEditEntityAction(boolean bot) {
		super(bot);
	}

	public void build() {
		log.info("[action=wbeditentity]: "
				+ ToStringBuilder.reflectionToString(this,
						ToStringStyle.SIMPLE_STYLE));

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setParameter(multipartEntity, "format", "json");

		setParameter(multipartEntity, "action", "wbeditentity");

		setParameter(multipartEntity, "id", id.toString());
		setParameter(multipartEntity, "site", site);
		setParameter(multipartEntity, "title", title);
		setParameter(multipartEntity, "baserevid", baserevid);
		setParameter(multipartEntity, "summary", summary);
		setParameter(multipartEntity, "token", token);
		setParameter(multipartEntity, "bot", bot);
		setParameter(multipartEntity, "data", data);
		setParameter(multipartEntity, "clear", clear);
		setParameter(multipartEntity, "new", new_);

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);
	}

	@Override
	protected void parseResult(String s) {
		JSONObject jsonObject = new JSONObject(s);

		if (jsonObject.has("error")) {
			throw new ProcessException(jsonObject.getJSONObject("error")
					.getString("info"));
		}
		if (!jsonObject.has("entity")) {
			throw new ProcessException("No 'entity' in response");
		}

		this.result = new ApiEntity(jsonObject.getJSONObject("entity"));
	}

}
