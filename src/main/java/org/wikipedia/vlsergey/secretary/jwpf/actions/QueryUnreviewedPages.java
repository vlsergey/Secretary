package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.FilterRedirects;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class QueryUnreviewedPages extends AbstractQueryAction implements MultiAction<Page> {

	private static final Log log = LogFactory.getLog(QueryUnreviewedPages.class);

	public static final int URLIMIT_FOR_BOTS = 5000;

	public static final int URLIMIT_FOR_NON_BOTS = 500;

	private final FilterRedirects filterRedirects;

	private final Namespace[] namespaces;

	private String nextStart;

	private List<Page> results;

	private final String urend;

	private final String urstart;

	public QueryUnreviewedPages(boolean bot, String urstart, String urend, Namespace[] namespaces,
			FilterRedirects filterRedirects) {
		super(bot);
		log.info("GetUnreviewedPages: " + urstart + "; " + urend + "; " + Arrays.toString(namespaces) + "; "
				+ filterRedirects);

		this.urstart = urstart;
		this.urend = urend;
		this.namespaces = namespaces;
		this.filterRedirects = filterRedirects;

		HttpPost postMethod = new HttpPost("/api.php");

		MultipartEntity multipartEntity = new MultipartEntity();
		setParameter(multipartEntity, "format", "xml");

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "list", "unreviewedpages");
		setParameter(multipartEntity, "urlimit", "" + getLimit());

		if (urstart != null)
			setParameter(multipartEntity, "urstart", urstart);

		if (urend != null)
			setParameter(multipartEntity, "urend", urend);

		if (filterRedirects != null)
			setParameter(multipartEntity, "urfilterredir", filterRedirects.toString());

		if (namespaces != null && namespaces.length > 0)
			setParameter(multipartEntity, "urnamespace", toStringParameters(namespaces));

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);

	}

	protected int getLimit() {
		return (isBot() ? URLIMIT_FOR_BOTS : URLIMIT_FOR_NON_BOTS);
	}

	@Override
	public MultiAction<Page> getNextAction() {
		if (nextStart == null)
			return null;

		return new QueryUnreviewedPages(isBot(), nextStart, urend, namespaces, filterRedirects);
	}

	@Override
	public Collection<Page> getResults() {
		return results;
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) throws ParseException {
		Element categorymembersElement = (Element) queryContinueElement.getElementsByTagName("unreviewedpages").item(0);
		nextStart = categorymembersElement.getAttribute("urstart");
	}

	@Override
	protected void parseQueryElement(Element queryElement) throws ProcessException, ParseException {
		results = new ArrayList<Page>();
		for (Element pElement : new ListAdapter<Element>(queryElement.getElementsByTagName("p"))) {
			ParsedPage p = parsePage(pElement);

			results.add(p);
		}

	}

}
