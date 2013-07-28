package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.Arrays;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByPageTitle extends AbstractQueryRevisionsAction implements MultiAction<ParsedPage> {

	private final Direction direction;

	private Long nextRevision;

	private final String pageTitle;

	private final RevisionPropery[] properties;

	public QueryRevisionsByPageTitle(boolean bot, String pageTitle, Long rvstartid, Direction direction,
			RevisionPropery[] properties) {
		super(bot, properties);

		log.info("queryRevisionsByPageTitle(" + pageTitle + "; " + direction + " ;" + Arrays.toString(properties)
				+ "; " + rvstartid + ")");

		this.pageTitle = pageTitle;
		this.direction = direction;
		this.properties = properties;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");
		setParameter(multipartEntity, "titles", pageTitle);

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

	@Override
	public MultiAction<ParsedPage> getNextAction() {
		if (nextRevision == null)
			return null;

		return new QueryRevisionsByPageTitle(isBot(), pageTitle, nextRevision, direction, properties);
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element revisionsElement = (Element) queryContinueElement.getElementsByTagName("revisions").item(0);
		nextRevision = new Long(revisionsElement.getAttribute("rvcontinue"));
	}

}
