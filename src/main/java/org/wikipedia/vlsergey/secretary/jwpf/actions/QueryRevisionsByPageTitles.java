package org.wikipedia.vlsergey.secretary.jwpf.actions;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByPageTitles extends AbstractQueryRevisionsAction {

	public QueryRevisionsByPageTitles(boolean bot, Iterable<? extends String> pageTitles, boolean followRedirects,
			RevisionPropery[] properties) {
		super(bot, properties);

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");

		if (followRedirects) {
			setParameter(multipartEntity, "redirects", "follow");
		}

		setParameter(multipartEntity, "titles", toStringParameters(pageTitles));
		setParameter(multipartEntity, "rvprop", toStringParameters(properties));

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

}
