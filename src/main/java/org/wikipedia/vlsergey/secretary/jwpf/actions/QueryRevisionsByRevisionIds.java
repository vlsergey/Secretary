package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.text.ParseException;
import java.util.Arrays;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByRevisionIds extends AbstractQueryRevisionsAction implements MultiAction<Page> {

	public static final int MAX_FOR_BOTS = 500;

	public static final int MAX_FOR_NON_BOTS = 50;

	private final boolean generateXml;

	private final RevisionPropery[] properties;

	private final Iterable<Long> revids;

	private Long rvcontinue;

	public QueryRevisionsByRevisionIds(boolean bot, Iterable<Long> revids, boolean generateXml,
			RevisionPropery[] properties) {
		this(bot, revids, generateXml, properties, null);
	}

	protected QueryRevisionsByRevisionIds(boolean bot, Iterable<Long> revids, boolean generateXml,
			RevisionPropery[] properties, Long rvcontinue) {
		super(bot, properties);

		log.info("queryRevisionsByRevisionIds( " + generateXml + ", " + Arrays.toString(properties) + ", " + rvcontinue
				+ " ): " + revids);

		this.revids = revids;
		this.generateXml = generateXml;
		this.properties = properties;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");

		/*
		 * A list of revision IDs to work on. Maximum number of values 50 (500
		 * for bots)
		 */
		setParameter(multipartEntity, "revids", toStringParameters(revids, MAX_FOR_NON_BOTS, MAX_FOR_BOTS));
		setParameter(multipartEntity, "rvprop", toStringParameters(properties));
		if (generateXml) {
			setParameter(multipartEntity, "rvgeneratexml", "1");
		}

		if (rvcontinue != null) {
			setParameter(multipartEntity, "rvcontinue", rvcontinue.toString());
		}

		setParameter(multipartEntity, "format", "xml");

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);
	}

	@Override
	public MultiAction<Page> getNextAction() {
		if (rvcontinue == null)
			return null;

		return new QueryRevisionsByRevisionIds(isBot(), revids, generateXml, properties, rvcontinue);
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) throws ParseException {
		Element revisionsElement = (Element) queryContinueElement.getElementsByTagName("revisions").item(0);
		rvcontinue = new Long(revisionsElement.getAttribute("rvcontinue"));
	}

}
