package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class QueryRedirectsByPageTitles extends AbstractQueryAction {

	public Map<String, String> redirects;

	public QueryRedirectsByPageTitles(boolean bot, Iterable<String> pageTitles) {

		super(bot);

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "redirects", "redirects");
		setParameter(multipartEntity, "titles", toStringParameters(pageTitles));
		setParameter(multipartEntity, "format", "xml");

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	public Map<String, String> getRedirects() {
		return redirects;
	}

	@Override
	protected void parseQueryElement(Element queryElement) throws ProcessException, ParseException {
		redirects = new LinkedHashMap<String, String>();

		for (Element redirectsElement : new ListAdapter<Element>(queryElement.getElementsByTagName("redirects"))) {
			for (Element rElement : new ListAdapter<Element>(redirectsElement.getElementsByTagName("r"))) {
				redirects.put(rElement.getAttribute("from"), rElement.getAttribute("to"));
			}
		}
	}
}
