package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.Arrays;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByLinks extends AbstractQueryRevisionsAction implements MultiAction<ParsedPage> {

	private String gplcontinue = null;

	private final Namespace[] namespaces;

	private final Long pageId;

	private final RevisionPropery[] properties;

	public QueryRevisionsByLinks(boolean bot, Long pageId, Namespace[] namespaces, RevisionPropery[] properties) {
		this(bot, pageId, namespaces, properties, null);
	}

	private QueryRevisionsByLinks(boolean bot, Long pageId, Namespace[] namespaces, RevisionPropery[] properties,
			String gplcontinue) {
		super(bot, properties);

		log.info("[action=query; prop=revisions; generator=links]: " + pageId + "; " + Arrays.toString(namespaces)
				+ "; " + Arrays.toString(properties) + "; " + gplcontinue);

		this.pageId = pageId;
		this.namespaces = namespaces;
		this.properties = properties;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");

		setParameter(multipartEntity, "generator", "links");
		setParameter(multipartEntity, "pageids", pageId);
		setParameter(multipartEntity, "gplnamespace", namespaces);
		setParameter(multipartEntity, "gpllimit", String.valueOf(bot ? 5000 : 500));

		if (gplcontinue != null) {
			setParameter(multipartEntity, "gplcontinue", gplcontinue);
		}

		setParameter(multipartEntity, "rvprop", toStringParameters(properties));

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	@Override
	public MultiAction<ParsedPage> getNextAction() {
		if (gplcontinue == null)
			return null;

		return new QueryRevisionsByLinks(isBot(), pageId, namespaces, properties, gplcontinue);
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element embeddedinElement = (Element) queryContinueElement.getElementsByTagName("links").item(0);
		gplcontinue = embeddedinElement.getAttribute("gplcontinue");
	}

}
