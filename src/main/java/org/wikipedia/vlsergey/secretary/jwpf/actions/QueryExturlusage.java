package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.ExternalUrl;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedExternalUrl;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class QueryExturlusage extends AbstractQueryAction implements MultiAction<ExternalUrl> {

	private final String namespaces;

	private String nextOffset = null;

	private final String protocol;

	private final String query;

	private List<ExternalUrl> result;

	public QueryExturlusage(boolean bot, String protocol, String query, String namespaces) {
		this(bot, protocol, query, namespaces, null);
	}

	private QueryExturlusage(boolean bot, String protocol, String query, String namespaces, String offset) {
		super(bot);

		log.info("queryExturlusage(" + protocol + "; " + query + "; " + namespaces + "; " + offset + ")");

		this.protocol = protocol;
		this.query = query;
		this.namespaces = namespaces;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "list", "exturlusage");

		setParameter(multipartEntity, "euprop", "ids|title|url");
		setParameter(multipartEntity, "euprotocol", protocol);
		setParameter(multipartEntity, "euquery", query);
		setParameter(multipartEntity, "eunamespace", namespaces);

		if (offset != null)
			setParameter(multipartEntity, "euoffset", offset);

		if (isBot()) {
			setParameter(multipartEntity, "eulimit", "5000");
		} else {
			setParameter(multipartEntity, "eulimit", "500");
		}

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	@Override
	public MultiAction<ExternalUrl> getNextAction() {
		if (nextOffset == null)
			return null;

		return new QueryExturlusage(isBot(), protocol, query, namespaces, nextOffset);
	}

	@Override
	public Collection<ExternalUrl> getResults() {
		return result;
	}

	private ExternalUrl parseExternalUrl(Element euElement) {
		ParsedExternalUrl externalUrl = new ParsedExternalUrl();

		if (euElement.hasAttribute("ns"))
			externalUrl.setNamespace(new Integer(euElement.getAttribute("ns")));

		if (euElement.hasAttribute("pageid"))
			externalUrl.setPageId(new Long(euElement.getAttribute("pageid")));

		if (euElement.hasAttribute("url"))
			externalUrl.setUrl(euElement.getAttribute("url"));

		if (euElement.hasAttribute("title"))
			externalUrl.setPageTitle(euElement.getAttribute("title"));

		return externalUrl;
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element exturlusage = (Element) queryContinueElement.getElementsByTagName("exturlusage").item(0);
		this.nextOffset = exturlusage.getAttribute("euoffset");
	}

	@Override
	protected void parseQueryElement(Element queryElement) throws ProcessException {
		final ListAdapter<Element> euElements = new ListAdapter<Element>(queryElement.getElementsByTagName("eu"));
		final List<ExternalUrl> result = new ArrayList<ExternalUrl>(euElements.size());

		for (Element euElement : euElements) {
			ExternalUrl pageImpl = parseExternalUrl(euElement);
			result.add(pageImpl);
		}

		this.result = result;
	}

}
