package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.text.ParseException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class QueryTokenEdit extends AbstractQueryAction {

	String editToken;

	public QueryTokenEdit(boolean bot, Revision revision) {
		super(bot);

		log.info("[action=query; prop=info; intoken=edit]: " + revision);

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "info");
		setParameter(multipartEntity, "revids", "" + revision.getId());
		setParameter(multipartEntity, "intoken", "edit");

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);
	}

	public QueryTokenEdit(boolean bot, String pageTitle) {
		super(bot);

		log.info("[action=query; prop=info; intoken=edit]: " + pageTitle);

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "info");
		setParameter(multipartEntity, "titles", pageTitle);
		setParameter(multipartEntity, "intoken", "edit");

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);
	}

	public String getEditToken() {
		return editToken;
	}

	@Override
	protected void parseQueryElement(org.w3c.dom.Element queryElement) throws ProcessException, ParseException {
		for (org.w3c.dom.Element cmPages : new ListAdapter<org.w3c.dom.Element>(
				queryElement.getElementsByTagName("pages"))) {
			for (org.w3c.dom.Element cmPage : new ListAdapter<org.w3c.dom.Element>(cmPages.getElementsByTagName("page"))) {
				editToken = cmPage.getAttribute("edittoken");
			}
		}
	}

}
