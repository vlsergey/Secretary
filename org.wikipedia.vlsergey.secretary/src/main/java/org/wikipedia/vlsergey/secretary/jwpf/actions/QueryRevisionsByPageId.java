package org.wikipedia.vlsergey.secretary.jwpf.actions;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByPageId extends AbstractQueryRevisionsAction
		implements MultiAction<Page> {

	private final Direction direction;

	private Long nextRevision;

	private final Long pageId;

	private final RevisionPropery[] properties;

	public QueryRevisionsByPageId(Long pageId, Long rvstartid,
			Direction direction, RevisionPropery[] properties) {
		super(properties);

		this.pageId = pageId;
		this.direction = direction;
		this.properties = properties;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");
		setParameter(multipartEntity, "pageids", "" + pageId);

		if (direction != null)
			setParameter(multipartEntity, "rvdir", direction.getQueryString());

		if (rvstartid != null)
			setParameter(multipartEntity, "rvstartid", "" + rvstartid);

		setParameter(multipartEntity, "rvlimit", "" + getLimit());
		setParameter(multipartEntity, "rvprop", toStringParameters(properties));
		setParameter(multipartEntity, "format", "xml");

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	public MultiAction<Page> getNextAction() {
		if (nextRevision == null)
			return null;

		return new QueryRevisionsByPageId(pageId, nextRevision, direction,
				properties);
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element revisionsElement = (Element) queryContinueElement
				.getElementsByTagName("revisions").item(0);
		nextRevision = new Long(revisionsElement.getAttribute("rvstartid"));
	}

}
