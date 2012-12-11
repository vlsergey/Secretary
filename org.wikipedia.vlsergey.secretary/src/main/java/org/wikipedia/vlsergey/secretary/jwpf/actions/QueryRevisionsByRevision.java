package org.wikipedia.vlsergey.secretary.jwpf.actions;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByRevision extends AbstractQueryRevisionsAction {

	public QueryRevisionsByRevision(boolean bot, Long revisionId, boolean rvgeneratexml, RevisionPropery[] properties) {
		super(bot, properties);

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");

		setParameter(multipartEntity, "revids", revisionId.toString());
		setParameter(multipartEntity, "rvprop", toStringParameters(properties));

		if (rvgeneratexml) {
			setParameter(multipartEntity, "rvgeneratexml", "1");
		}

		setParameter(multipartEntity, "format", "xml");

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	public QueryRevisionsByRevision(boolean bot, Long revisionId, RevisionPropery[] properties) {
		this(bot, revisionId, false, properties);
	}

}
