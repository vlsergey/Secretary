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

	private final String pageTitle;

	private final RevisionPropery[] properties;

	private String rvcontinue;

	private final Long rvstartid;

	public QueryRevisionsByPageTitle(boolean bot, String pageTitle, Long rvstartid, Direction direction,
			RevisionPropery[] properties) {
		this(bot, pageTitle, rvstartid, direction, properties, null);
	}

	private QueryRevisionsByPageTitle(boolean bot, String pageTitle, Long rvstartid, Direction direction,
			RevisionPropery[] properties, String rvcontinue) {
		super(bot, properties);

		log.info("queryRevisionsByPageTitle(" + pageTitle + "; " + direction + " ;" + Arrays.toString(properties)
				+ "; " + rvstartid + "; " + rvcontinue + ")");

		this.pageTitle = pageTitle;
		this.rvstartid = rvstartid;
		this.direction = direction;
		this.properties = properties;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");
		setParameter(multipartEntity, "titles", pageTitle);
		setParameter(multipartEntity, "rvdir", direction.getQueryString());
		setParameter(multipartEntity, "rvstartid", rvstartid);
		setParameter(multipartEntity, "rvlimit", getLimit());
		setParameter(multipartEntity, "rvprop", toStringParameters(properties));
		setParameter(multipartEntity, "rvcontinue", rvcontinue);

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	@Override
	public MultiAction<ParsedPage> getNextAction() {
		if (rvcontinue == null)
			return null;

		return new QueryRevisionsByPageTitle(isBot(), pageTitle, rvstartid, direction, properties, rvcontinue);
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element revisionsElement = (Element) queryContinueElement.getElementsByTagName("revisions").item(0);
		rvcontinue = revisionsElement.getAttribute("rvcontinue");
	}

}
