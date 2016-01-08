package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.RecentChangePropery;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByRecentChanges extends AbstractQueryRevisionsAction implements Cloneable,
		MultiAction<ParsedPage> {

	public String grccontinue;

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
	public Direction grcdir;

	/**
	 * The timestamp to end enumerating
	 */
	public Date grcend;

	/**
	 * Don't list changes by this user
	 */
	public String grcexcludeuser;

	/**
	 * Filter log entries to only this namespace(s)
	 */
	public Namespace[] grcnamespace;

	/**
	 * Include additional pieces of information. Default: title|timestamp|ids
	 */
	public RecentChangePropery grcprop;

	public String grcshow;

	/**
	 * The timestamp to start enumerating from
	 */
	public Date grcstart;

	public String grctag;

	public Boolean grctoponly;

	public String grctype;

	/**
	 * Only list changes by this user
	 */
	public String grcuser;

	private String newGrccontinue;

	public QueryRevisionsByRecentChanges(boolean bot, final List<RevisionPropery> properties) {
		super(bot, properties);
	}

	public QueryRevisionsByRecentChanges(boolean bot, final RevisionPropery[] properties) {
		super(bot, properties);
	}

	public void build() {
		log.info("[action=query; list=recentchanges]: "
				+ ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE));

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setParameter(multipartEntity, "format", "xml");

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");
		setParameter(multipartEntity, "generator", "recentchanges");

		setParameter(multipartEntity, "grcstart", grcstart);
		setParameter(multipartEntity, "grcend", grcend);
		setParameter(multipartEntity, "grcdir", grcdir);
		setParameter(multipartEntity, "grcnamespace", grcnamespace);
		setParameter(multipartEntity, "grcuser", grcuser);
		setParameter(multipartEntity, "grcexcludeuser", grcexcludeuser);
		setParameter(multipartEntity, "grctag", grctag);
		setParameter(multipartEntity, "grcprop", grcprop);
		setParameter(multipartEntity, "grcshow", grcshow);
		setParameter(multipartEntity, "grclimit", isBot() ? 5000 : 500);
		setParameter(multipartEntity, "grctype", grctype);
		setParameter(multipartEntity, "grctoponly", grctoponly);
		setParameter(multipartEntity, "grccontinue", grccontinue);

		setParameter(multipartEntity, "rvprop", toStringParameters(properties));

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);
	}

	@Override
	protected QueryRevisionsByRecentChanges clone() {
		try {
			return (QueryRevisionsByRecentChanges) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public MultiAction<ParsedPage> getNextAction() {
		if (StringUtils.isBlank(newGrccontinue)) {
			return null;
		}
		QueryRevisionsByRecentChanges queryRecentChanges = this.clone();
		queryRecentChanges.reset();
		queryRecentChanges.grccontinue = this.newGrccontinue;
		queryRecentChanges.build();
		return queryRecentChanges;
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element embeddedinElement = (Element) queryContinueElement.getElementsByTagName("recentchanges").item(0);
		this.newGrccontinue = embeddedinElement.getAttribute("grccontinue");
	}

	@Override
	public void reset() {
		super.reset();
		this.newGrccontinue = null;
	}

}
