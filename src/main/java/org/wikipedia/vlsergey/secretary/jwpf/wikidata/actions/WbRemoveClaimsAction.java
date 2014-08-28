package org.wikipedia.vlsergey.secretary.jwpf.wikidata.actions;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.actions.AbstractApiAction;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

/**
 * API module to create a single new Wikibase entity and modify it with
 * serialised information.
 */
public class WbRemoveClaimsAction extends AbstractApiAction {

	/**
	 * The numeric identifier for the revision to base the modification on. This
	 * is used for detecting conflicts during save.
	 */
	public Long baserevid;

	/**
	 * Mark this edit as bot This URL flag will only be respected if the user
	 * belongs to the group "bot".
	 */
	public Boolean bot;

	/**
	 * One GUID or several (pipe-separated) GUIDs identifying the claims to be
	 * removed. All claims must belong to the same entity.
	 */
	public String[] claim;

	/**
	 * Summary for the edit. Will be prepended by an automatically generated
	 * comment. The length limit of the autocomment together with the summary is
	 * 260 characters. Be aware that everything above that limit will be cut
	 * off.
	 */
	public String summary;

	/**
	 * A "edittoken" token previously obtained through the token module (
	 * {@link MediaWikiBot#queryTokenEdit(String)} )
	 */
	public String token;

	public WbRemoveClaimsAction(boolean bot) {
		super(bot);
	}

	public void build() {
		log.info("[action=wbeditentity]: " + ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE));

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setParameter(multipartEntity, "format", "json");

		setParameter(multipartEntity, "action", "wbremoveclaims");

		setParameter(multipartEntity, "baserevid", baserevid);
		setParameter(multipartEntity, "summary", summary);
		setParameter(multipartEntity, "token", token);
		setParameter(multipartEntity, "bot", bot);
		setParameter(multipartEntity, "claim", claim);

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);
	}

	@Override
	protected void parseResult(String s) {
		JSONObject jsonObject = new JSONObject(s);
		if (jsonObject.has("error")) {
			throw new ProcessException(jsonObject.getJSONObject("error").getString("info"));
		}
	}

}
