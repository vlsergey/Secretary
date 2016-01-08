package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.Arrays;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.FilterRedirects;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByBacklinks extends AbstractQueryRevisionsAction implements MultiAction<ParsedPage> {

	private final FilterRedirects filterredir;

	private String gblcontinue = null;

	private final Namespace[] namespaces;

	private final Long pageId;

	private final RevisionPropery[] properties;

	public QueryRevisionsByBacklinks(boolean bot, Long pageId, Namespace[] namespaces, FilterRedirects filterredir,
			RevisionPropery[] properties) {
		this(bot, pageId, namespaces, filterredir, properties, null);
	}

	private QueryRevisionsByBacklinks(boolean bot, Long pageId, Namespace[] namespaces, FilterRedirects filterredir,
			RevisionPropery[] properties, String gplcontinue) {
		super(bot, properties);

		log.info("[action=query; prop=revisions; generator=backlinks]: " + pageId + "; " + Arrays.toString(namespaces)
				+ "; " + filterredir + ";" + Arrays.toString(properties) + "; " + gplcontinue);

		this.pageId = pageId;
		this.namespaces = namespaces;
		this.filterredir = filterredir;
		this.properties = properties;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");

		setParameter(multipartEntity, "generator", "backlinks");
		setParameter(multipartEntity, "gblpageid", pageId);
		setParameter(multipartEntity, "gblnamespace", namespaces);
		setParameter(multipartEntity, "gblfilterredir", filterredir);
		setParameter(multipartEntity, "gbllimit", String.valueOf(bot ? 5000 : 500));

		if (gplcontinue != null) {
			setParameter(multipartEntity, "gblcontinue", gplcontinue);
		}

		setParameter(multipartEntity, "rvprop", toStringParameters(properties));

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	@Override
	public MultiAction<ParsedPage> getNextAction() {
		if (gblcontinue == null)
			return null;

		return new QueryRevisionsByBacklinks(isBot(), pageId, namespaces, filterredir, properties, gblcontinue);
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element embeddedinElement = findAnyChildElementNode(queryContinueElement, "backlinks").get();
		gblcontinue = embeddedinElement.getAttribute("gblcontinue");
	}

}
