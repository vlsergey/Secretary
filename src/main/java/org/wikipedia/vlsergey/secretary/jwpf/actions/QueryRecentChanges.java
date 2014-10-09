package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.RecentChange;
import org.wikipedia.vlsergey.secretary.jwpf.model.RecentChangePropery;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class QueryRecentChanges extends AbstractQueryAction implements Cloneable, MultiAction<RecentChange> {

	private String newRccontinue;

	public String rccontinue;

	/**
	 * In which direction to enumerate
	 * <ul>
	 * <li>newer - List oldest first. Note: rcstart has to be before rcend.
	 * <li>older - List newest first (default). Note: rcstart has to be later
	 * than rcend.
	 * </ul>
	 * 
	 * Default: older
	 */
	public Direction rcdir;

	/**
	 * The timestamp to end enumerating
	 */
	public Date rcend;

	/**
	 * Don't list changes by this user
	 */
	public String rcexcludeuser;

	/**
	 * Filter log entries to only this namespace(s)
	 */
	public Namespace[] rcnamespace;

	/**
	 * Include additional pieces of information. Default: title|timestamp|ids
	 */
	public RecentChangePropery rcprop;

	public String rcshow;

	/**
	 * The timestamp to start enumerating from
	 */
	public Date rcstart;

	public String rctag;

	public Boolean rctoponly;

	public String rctype;

	/**
	 * Only list changes by this user
	 */
	public String rcuser;

	private List<RecentChange> recentChanges;

	public QueryRecentChanges(boolean bot) {
		super(bot);
	}

	public void build() {
		log.info("[action=query; list=recentchanges]: "
				+ ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE));

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setParameter(multipartEntity, "format", "xml");

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "list", "recentchanges");

		setParameter(multipartEntity, "rcstart", rcstart);
		setParameter(multipartEntity, "rcend", rcend);
		setParameter(multipartEntity, "rcdir", rcdir);
		setParameter(multipartEntity, "rcnamespace", rcnamespace);
		setParameter(multipartEntity, "rcuser", rcuser);
		setParameter(multipartEntity, "rcexcludeuser", rcexcludeuser);
		setParameter(multipartEntity, "rctag", rctag);
		setParameter(multipartEntity, "rcprop", rcprop);
		setParameter(multipartEntity, "rcshow", rcshow);
		setParameter(multipartEntity, "rclimit", isBot() ? 5000 : 500);
		setParameter(multipartEntity, "rctype", rctype);
		setParameter(multipartEntity, "rctoponly", rctoponly);
		setParameter(multipartEntity, "rccontinue", rccontinue);

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);
	}

	@Override
	protected QueryRecentChanges clone() {
		try {
			return (QueryRecentChanges) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MultiAction<RecentChange> getNextAction() {
		if (StringUtils.isBlank(newRccontinue)) {
			return null;
		}
		QueryRecentChanges queryRecentChanges = this.clone();
		queryRecentChanges.reset();
		queryRecentChanges.rccontinue = this.newRccontinue;
		queryRecentChanges.build();
		return queryRecentChanges;
	}

	@Override
	public Collection<RecentChange> getResults() {
		return this.recentChanges;
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) throws ParseException {
		Element revisionsElement = (Element) queryContinueElement.getElementsByTagName("recentchanges").item(0);
		this.newRccontinue = revisionsElement.getAttribute("rccontinue");
	}

	@Override
	protected void parseQueryElement(Element queryElement) throws ProcessException, ParseException {
		this.recentChanges = new ArrayList<RecentChange>();

		Element recentchangesElements = (Element) queryElement.getElementsByTagName("recentchanges").item(0);
		for (Element rcElement : new ListAdapter<Element>(recentchangesElements.getElementsByTagName("rc"))) {
			RecentChange recentChange = new RecentChange();

			if (rcElement.hasAttribute("ns"))
				recentChange.ns = new Integer(rcElement.getAttribute("ns"));
			if (rcElement.hasAttribute("pageid"))
				recentChange.pageid = new Long(rcElement.getAttribute("pageid"));
			if (rcElement.hasAttribute("revid"))
				recentChange.revid = new Long(rcElement.getAttribute("revid"));
			if (rcElement.hasAttribute("timestamp"))
				recentChange.timestamp = parseDate(rcElement.getAttribute("timestamp"));
			if (rcElement.hasAttribute("title"))
				recentChange.title = rcElement.getAttribute("title");

			this.recentChanges.add(recentChange);
		}
	}
}
