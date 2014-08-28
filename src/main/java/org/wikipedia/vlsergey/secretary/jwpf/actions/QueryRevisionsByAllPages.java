package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.Arrays;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByAllPages extends AbstractQueryRevisionsAction implements MultiAction<ParsedPage> {

	private String gapcontinue = null;

	private final Namespace namespace;

	private final RevisionPropery[] properties;

	public QueryRevisionsByAllPages(boolean bot, Namespace namespace, RevisionPropery[] properties) {
		this(bot, namespace, properties, null);
	}

	private QueryRevisionsByAllPages(boolean bot, Namespace namespace, RevisionPropery[] properties, String gapcontinue) {
		super(bot, properties);

		log.info("[action=query; prop=revisions; generator=allpages]: " + namespace + "; " + "; "
				+ Arrays.toString(properties) + "; " + gapcontinue);

		this.namespace = namespace;
		this.properties = properties;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");

		setParameter(multipartEntity, "generator", "allpages");
		setParameter(multipartEntity, "gapnamespace", namespace);
		setParameter(multipartEntity, "gaplimit", String.valueOf(bot ? 5000 : 500));
		setParameter(multipartEntity, "gapcontinue", gapcontinue);

		setParameter(multipartEntity, "rvprop", toStringParameters(properties));

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	@Override
	public MultiAction<ParsedPage> getNextAction() {
		if (gapcontinue == null)
			return null;

		return new QueryRevisionsByAllPages(isBot(), namespace, properties, gapcontinue);
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element embeddedinElement = (Element) queryContinueElement.getElementsByTagName("allpages").item(0);
		gapcontinue = embeddedinElement.getAttribute("gapcontinue");
	}

}
