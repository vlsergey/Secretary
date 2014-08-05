package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.UserContributionItem;
import org.wikipedia.vlsergey.secretary.jwpf.model.UserContributionProperty;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class QueryUserContributions extends AbstractQueryAction implements MultiAction<UserContributionItem> {

	private static final Log log = LogFactory.getLog(QueryUserContributions.class);

	public static final int URLIMIT_FOR_BOTS = 5000;

	public static final int URLIMIT_FOR_NON_BOTS = 500;

	private List<UserContributionItem> results;

	private String uccontinue;

	private final Direction ucdir;

	private final Date ucend;

	private final Namespace[] ucnamespace;

	private final UserContributionProperty[] ucprop;

	private final Date ucstart;

	private final String[] ucuser;

	private final String ucuserprefix;

	public QueryUserContributions(boolean bot, Date ucstart, Date ucend, String[] ucuser, String ucuserprefix,
			Direction ucdir, Namespace[] ucnamespace, UserContributionProperty[] ucprop) {
		this(bot, ucstart, ucend, ucuser, ucuserprefix, ucdir, ucnamespace, ucprop, null);
	}

	private QueryUserContributions(boolean bot, Date ucstart, Date ucend, String[] ucuser, String ucuserprefix,
			Direction ucdir, Namespace[] ucnamespace, UserContributionProperty[] ucprop, String uccontinue) {
		super(bot);

		log.info("[action=query; list=usercontribs]: " + ucstart + "; " + ucend + "; " + Arrays.toString(ucuser) + "; "
				+ ucuserprefix + "; " + ucdir + "; " + Arrays.toString(ucnamespace) + "; " + Arrays.toString(ucprop)
				+ "; " + uccontinue);

		this.ucstart = ucstart;
		this.ucend = ucend;
		this.ucuser = ucuser;
		this.ucuserprefix = ucuserprefix;
		this.ucdir = ucdir;
		this.ucnamespace = ucnamespace;
		this.ucprop = ucprop;
		this.uccontinue = null;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "list", "usercontribs");
		setParameter(multipartEntity, "uclimit", "" + getLimit());

		if (ucstart != null)
			setParameter(multipartEntity, "urstart", ucstart);
		if (ucend != null)
			setParameter(multipartEntity, "ucend", ucend);
		if (ucuser != null)
			setParameter(multipartEntity, "ucuser", toStringParameters(ucuser));
		if (ucuserprefix != null)
			setParameter(multipartEntity, "ucuserprefix", ucuserprefix);
		if (ucdir != null)
			setParameter(multipartEntity, "ucdir", ucdir.getQueryString());
		if (ucnamespace != null && ucnamespace.length > 0)
			setParameter(multipartEntity, "ucnamespace", toStringParameters(ucnamespace));
		if (ucprop != null && ucprop.length > 0)
			setParameter(multipartEntity, "ucprop", toStringParameters(ucprop));
		if (uccontinue != null)
			setParameter(multipartEntity, "uccontinue", uccontinue);

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);

	}

	protected int getLimit() {
		return (isBot() ? URLIMIT_FOR_BOTS : URLIMIT_FOR_NON_BOTS);
	}

	@Override
	public MultiAction<UserContributionItem> getNextAction() {
		if (uccontinue == null)
			return null;

		return new QueryUserContributions(bot, ucstart, ucend, ucuser, ucuserprefix, ucdir, ucnamespace, ucprop,
				uccontinue);
	}

	@Override
	public Collection<UserContributionItem> getResults() {
		return results;
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) throws ParseException {
		Element categorymembersElement = (Element) queryContinueElement.getElementsByTagName("usercontribs").item(0);
		uccontinue = categorymembersElement.getAttribute("uccontinue");
	}

	@Override
	protected void parseQueryElement(Element queryElement) throws ProcessException, ParseException {
		results = new ArrayList<UserContributionItem>();
		for (Element itemElement : new ListAdapter<Element>(queryElement.getElementsByTagName("item"))) {
			UserContributionItem item = parseUserContributionItem(itemElement);
			results.add(item);
		}

	}

	private UserContributionItem parseUserContributionItem(Element itemElement) {
		UserContributionItem item = new UserContributionItem();

		if (itemElement.hasAttribute("comment"))
			item.comment = itemElement.getAttribute("comment");
		if (itemElement.hasAttribute("minor"))
			item.minor = StringUtils.isNotEmpty(itemElement.getAttribute("minor"));
		if (itemElement.hasAttribute("ns"))
			item.ns = Integer.parseInt(itemElement.getAttribute("ns"));
		if (itemElement.hasAttribute("pageid"))
			item.pageid = Long.parseLong(itemElement.getAttribute("pageid"));
		if (itemElement.hasAttribute("parentid"))
			item.parentid = Long.parseLong(itemElement.getAttribute("parentid"));
		if (itemElement.hasAttribute("revid"))
			item.revid = Long.parseLong(itemElement.getAttribute("revid"));
		if (itemElement.hasAttribute("size"))
			item.size = Long.parseLong(itemElement.getAttribute("size"));
		if (itemElement.hasAttribute("timestamp")) {
			try {
				item.timestamp = parseDate(itemElement.getAttribute("timestamp"));
			} catch (ParseException exc) {
				throw new ProcessException(exc.getMessage(), exc);
			}
		}
		if (itemElement.hasAttribute("title"))
			item.title = itemElement.getAttribute("title");
		if (itemElement.hasAttribute("top"))
			item.top = StringUtils.isNotEmpty(itemElement.getAttribute("top"));
		if (itemElement.hasAttribute("user"))
			item.user = itemElement.getAttribute("user");
		if (itemElement.hasAttribute("userid"))
			item.userid = Long.parseLong(itemElement.getAttribute("userid"));

		return item;
	}

}
