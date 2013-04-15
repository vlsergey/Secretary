package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.Arrays;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByEmbeddedIn extends AbstractQueryRevisionsAction implements MultiAction<Page> {

	private final String embeddedIn;

	private String geicontinue = null;

	private final String namespaces;

	private final RevisionPropery[] properties;

	public QueryRevisionsByEmbeddedIn(boolean bot, String embeddedIn, String namespaces, RevisionPropery[] properties) {
		this(bot, embeddedIn, namespaces, properties, null);
	}

	private QueryRevisionsByEmbeddedIn(boolean bot, String embeddedIn, String namespaces, RevisionPropery[] properties,
			String geicontinue) {
		super(bot, properties);

		log.info("queryRevisionsByEmbeddedIn(" + embeddedIn + "; " + namespaces + " ;" + Arrays.toString(properties)
				+ "; " + geicontinue + ")");

		this.embeddedIn = embeddedIn;
		this.namespaces = namespaces;
		this.properties = properties;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");

		setParameter(multipartEntity, "generator", "embeddedin");
		setParameter(multipartEntity, "geititle", embeddedIn);
		setParameter(multipartEntity, "geinamespace", namespaces);
		setParameter(multipartEntity, "geilimit", String.valueOf(bot ? 5000 : 500));

		if (geicontinue != null) {
			setParameter(multipartEntity, "geicontinue", geicontinue);
		}

		setParameter(multipartEntity, "rvprop", toStringParameters(properties));
		setParameter(multipartEntity, "format", "xml");

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	@Override
	public MultiAction<Page> getNextAction() {
		if (geicontinue == null)
			return null;

		return new QueryRevisionsByEmbeddedIn(isBot(), embeddedIn, namespaces, properties, geicontinue);
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element embeddedinElement = (Element) queryContinueElement.getElementsByTagName("embeddedin").item(0);
		geicontinue = embeddedinElement.getAttribute("geicontinue");
	}

}
