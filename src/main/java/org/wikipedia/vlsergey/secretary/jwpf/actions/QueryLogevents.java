package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.LogItem;

/**
 * List log events, filtered by time range, event type, user type, or the page
 * it applies to. Ordered by event timestamp. Parameters: letype (flt), lefrom
 * (paging timestamp), leto (flt), ledirection (dflt=older), leuser (flt),
 * letitle (flt), lelimit (dflt=10, max=500/5000)
 * 
 * api.php ? action=query & list=logevents - List last 10 events of any type
 * 
 * TODO This is a semi-complete extension point
 * 
 * @author Thomas Stock
 * @supportedBy MediaWikiAPI 1.11 logevents / le (semi-complete)
 * 
 */
public class QueryLogevents extends AbstractQueryAction implements MultiAction<LogItem> {

	private static final Log log = LogFactory.getLog(QueryLogevents.class);

	public static final int MAX_FOR_BOTS = 5000;

	public static final int MAX_FOR_NON_BOTS = 500;

	private final Direction ledir;

	private final String leend;

	private final String letitle;

	private final String[] letype;

	private final String leuser;

	private String nextLEStart = null;

	private Collection<LogItem> results = new ArrayList<LogItem>(getLimit());

	/**
	 * @param letitle
	 *            Filter entries to those related to a page
	 * @param leuser
	 *            Filter entries to those made by the given user
	 * @param lestart
	 *            The timestamp to start enumerating from
	 * @param leend
	 *            The timestamp to end enumerating
	 * @param ledir
	 *            In which direction to enumerate
	 * @param letype
	 *            Filter log entries to only this type. Can be empty, or One
	 *            value: block, protect, rights, delete, upload, move, import,
	 *            patrol, merge, suppress, review, stable, gblblock, renameuser,
	 *            globalauth, gblrights, abusefilter, newusers
	 */
	public QueryLogevents(boolean bot, String letitle, String leuser, String lestart, String leend, Direction ledir,
			String... letype) {
		super(bot);

		if (log.isInfoEnabled())
			log.info("GetLogEvents: " + letitle + "; " + leuser + "; " + lestart + "; " + leend + "; "
					+ Arrays.toString(letype));

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();

		this.ledir = ledir;
		this.leend = leend;
		this.letitle = letitle;
		this.letype = letype;
		this.leuser = leuser;

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "list", "logevents");
		setParameter(multipartEntity, "format", "xml");

		if (ledir != null)
			setParameter(multipartEntity, "ledir", ledir.getQueryString());

		if (letitle != null)
			setParameter(multipartEntity, "letitle", letitle);

		if (leuser != null)
			setParameter(multipartEntity, "leuser", leuser);

		if (lestart != null)
			setParameter(multipartEntity, "lestart", lestart);

		if (lestart != null)
			setParameter(multipartEntity, "leend", leend);

		if (letype.length > 0) {
			setParameter(multipartEntity, "letype", toStringParameters(Arrays.asList(letype)));
		}

		setParameter(multipartEntity, "lelimit", "" + getLimit());

		msgs.add(postMethod);
	}

	protected int getLimit() {
		return (isBot() ? MAX_FOR_BOTS : MAX_FOR_NON_BOTS);
	}

	@Override
	public QueryLogevents getNextAction() {
		if (nextLEStart == null)
			return null;

		return new QueryLogevents(isBot(), letitle, leuser, nextLEStart, leend, ledir, letype);
	}

	@Override
	public Collection<LogItem> getResults() {
		return results;
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element logeventsElement = (Element) queryContinueElement.getElementsByTagName("logevents").item(0);
		nextLEStart = logeventsElement.getAttribute("lestart");
	}

	@Override
	protected void parseQueryElement(Element queryElement) throws ParseException {
		Element logEventsElements = (Element) queryElement.getElementsByTagName("logevents").item(0);
		for (Element itemElement : new ListAdapter<Element>(logEventsElements.getElementsByTagName("item"))) {
			LogItem logItem = new LogItem();

			if (itemElement.hasAttribute("action"))
				logItem.setAction(itemElement.getAttribute("action"));

			if (itemElement.hasAttribute("comment"))
				logItem.setComment(itemElement.getAttribute("comment"));

			if (itemElement.hasAttribute("ns"))
				logItem.setNs(new Integer(itemElement.getAttribute("ns")));

			if (itemElement.hasAttribute("pageid"))
				logItem.setPageID(new Long(itemElement.getAttribute("pageid")));

			if (itemElement.hasAttribute("timestamp"))
				logItem.setTimestamp(parseDate(itemElement.getAttribute("timestamp")));

			if (itemElement.hasAttribute("title"))
				logItem.setTitle(itemElement.getAttribute("title"));

			if (itemElement.hasAttribute("type"))
				logItem.setType(itemElement.getAttribute("type"));

			if (itemElement.hasAttribute("user"))
				logItem.setUser(itemElement.getAttribute("user"));

			for (Element patrolElement : new ListAdapter<Element>(itemElement.getElementsByTagName("patrol"))) {

				if (patrolElement.hasAttribute("auto"))
					logItem.setPatrolAuto(!"0".equals(patrolElement.getAttribute("auto")));

				if (patrolElement.hasAttribute("cur"))
					logItem.setPatrolCurID(new Long(patrolElement.getAttribute("cur")));
			}

			final ListAdapter<Element> params = new ListAdapter<Element>(itemElement.getElementsByTagName("param"));
			if (!params.isEmpty()) {
				logItem.setParams(new ArrayList<String>(params.size()));
			}
			for (Element paramElement : params) {
				logItem.getParams().add(paramElement.getChildNodes().item(0).getNodeValue());
			}

			results.add(logItem);
		}

	}

}
