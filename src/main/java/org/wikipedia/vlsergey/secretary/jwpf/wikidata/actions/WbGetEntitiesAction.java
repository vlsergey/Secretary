package org.wikipedia.vlsergey.secretary.jwpf.wikidata.actions;

import java.util.LinkedHashMap;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.jwpf.actions.AbstractApiAction;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityProperty;

public class WbGetEntitiesAction extends AbstractApiAction {

	private static final String ID_MISSING = "-1";

	/**
	 * The sort order for the given properties. Use together with {@link #sort}
	 * to give the properties to sort. Note that this will change due to name
	 * clash (ie. {@link #dir} should work on all entities).
	 */
	public Direction dir;

	/**
	 * The IDs of the entities to get the data from
	 */
	public String[] ids;

	/**
	 * Apply language fallback for languages defined in the "languages"
	 * parameter, with the current context of API call.
	 */
	public String languagefallback;

	public String[] languages;

	/**
	 * Try to normalize the page title against the client site. This only works
	 * if exactly one site and one page have been given.
	 */
	public Boolean normalize;

	/**
	 * The names of the properties to get back from each entity. Will be further
	 * filtered by any languages given.
	 */
	public EntityProperty[] props;

	public LinkedHashMap<String, Entity> result;

	/**
	 * Filter sitelinks in entities to those with these siteids.
	 */
	public String[] sitefilter;

	/**
	 * Identifier for the site on which the corresponding page resides
	 */
	public String[] sites;

	/**
	 * The names of the properties to sort. Use together with 'dir' to give the
	 * sort order.
	 */
	public EntityProperty sort;

	/**
	 * The title of the corresponding page. Use together with {@link #sites},
	 * but only give one site for several titles or several sites for one title.
	 */
	public String[] titles;

	/**
	 * Do not group snaks by property id.
	 */
	public Boolean ungroupedlist;

	public WbGetEntitiesAction(boolean bot) {
		super(bot);
	}

	public void build() {
		log.info("[action=wbgetentities]: " + ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE));

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setParameter(multipartEntity, "format", "json");

		setParameter(multipartEntity, "action", "wbgetentities");

		setParameter(multipartEntity, "dir", dir);
		setParameter(multipartEntity, "ids", ids);
		setParameter(multipartEntity, "languagefallback", languagefallback);
		setParameter(multipartEntity, "languages", languages);
		setParameter(multipartEntity, "normalize", normalize);
		setParameter(multipartEntity, "props", props);
		setParameter(multipartEntity, "sitefilter", sitefilter);
		setParameter(multipartEntity, "sites", sites);
		setParameter(multipartEntity, "sort", sort);
		setParameter(multipartEntity, "titles", titles);
		setParameter(multipartEntity, "ungroupedlist", ungroupedlist);

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);
	}

	@Override
	protected void parseResult(String s) {
		JSONObject jsonObject = new JSONObject(s);

		if (jsonObject.has("error")) {
			throw new ProcessException(jsonObject.getJSONObject("error").getString("info"));
		}
		if (!jsonObject.has("entities")) {
			throw new ProcessException("No 'entities' in response");
		}

		this.result = new LinkedHashMap<>();
		JSONObject entities = jsonObject.getJSONObject("entities");
		for (Object key : entities.keySet()) {
			if (ID_MISSING.equals(key)) {
				// missing
				continue;
			}

			this.result.put(key.toString(), new Entity(entities.getJSONObject(key.toString())));
		}
	}

}
