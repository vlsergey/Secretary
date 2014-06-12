package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.User;
import org.wikipedia.vlsergey.secretary.jwpf.model.UserProperty;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class QueryAllusers extends AbstractQueryAction implements MultiAction<User> {

	private static final Log log = LogFactory.getLog(QueryAllusers.class);

	public static final int MAX_FOR_BOTS = 5000;

	public static final int MAX_FOR_NON_BOTS = 500;

	private final Boolean auactiveusers;
	private final String audir;
	private final Collection<String> auexcludegroup;
	private final String aufrom;
	private final Collection<String> augroup;
	private final String auprefix;
	private final Collection<UserProperty> auprop;
	private final Collection<String> aurights;
	private final String auto;
	private final Boolean auwitheditsonly;
	private String nextAufrom = null;
	private List<User> result;

	public QueryAllusers(boolean bot, String aufrom, String auto, String auprefix, String audir,
			Collection<String> augroup, Collection<String> auexcludegroup, Collection<String> aurights,
			Collection<UserProperty> auprop, Boolean auwitheditsonly, Boolean auactiveusers) {
		super(bot);
		this.aufrom = aufrom;
		this.auto = auto;
		this.auprefix = auprefix;
		this.audir = audir;
		this.augroup = augroup;
		this.auexcludegroup = auexcludegroup;
		this.aurights = aurights;
		this.auprop = auprop;
		this.auwitheditsonly = auwitheditsonly;
		this.auactiveusers = auactiveusers;

		if (log.isInfoEnabled())
			log.info("QueryAllusers");

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "list", "allusers");

		if (aufrom != null)
			setParameter(multipartEntity, "aufrom", aufrom);
		if (auto != null)
			setParameter(multipartEntity, "auto", auto);
		if (auprefix != null)
			setParameter(multipartEntity, "auprefix", auprefix);
		if (audir != null)
			setParameter(multipartEntity, "audir", audir);
		if (augroup != null)
			setParameter(multipartEntity, "augroup", toStringParameters(augroup));
		if (auexcludegroup != null)
			setParameter(multipartEntity, "auexcludegroup", toStringParameters(auexcludegroup));
		if (aurights != null)
			setParameter(multipartEntity, "aurights", toStringParameters(aurights));
		if (auprop != null)
			setParameter(multipartEntity, "auprop", toStringParameters(auprop));
		if (auwitheditsonly != null)
			setParameter(multipartEntity, "auwitheditsonly", "" + auwitheditsonly);
		if (auactiveusers != null)
			setParameter(multipartEntity, "auactiveusers", "" + auactiveusers);

		setParameter(multipartEntity, "aulimit", "" + getLimit());
		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	protected int getLimit() {
		return (isBot() ? MAX_FOR_BOTS : MAX_FOR_NON_BOTS);
	}

	@Override
	public MultiAction<User> getNextAction() {

		if (nextAufrom == null) {
			return null;
		}

		return new QueryAllusers(isBot(), nextAufrom, auto, auprefix, audir, augroup, auexcludegroup, aurights, auprop,
				auwitheditsonly, auactiveusers);
	}

	@Override
	public Collection<User> getResults() {
		return result;
	}

	@Override
	protected void parseQueryElement(Element queryElement) throws ProcessException, ParseException {
		final ListAdapter<Element> uElements = new ListAdapter<Element>(queryElement.getElementsByTagName("u"));
		final List<User> result = new ArrayList<User>(uElements.size());

		for (Element uElement : uElements) {
			User user = parseUser(uElement);
			result.add(user);
		}

		this.result = result;
	}

	private User parseUser(Element uElement) {

		User user = new User();

		if (uElement.hasAttribute("userid")) {
			try {
				String string = uElement.getAttribute("userid");
				Long l = Long.valueOf(string);
				user.setUserId(l);
			} catch (NumberFormatException exc) {
				throw new ProcessException(exc.getMessage(), exc);
			}
		}

		if (uElement.hasAttribute("name")) {
			user.setName(uElement.getAttribute("name"));
		}

		if (uElement.hasAttribute("editcount")) {
			try {
				String string = uElement.getAttribute("editcount");
				Long l = Long.valueOf(string);
				user.setEditcount(l);
			} catch (NumberFormatException exc) {
				throw new ProcessException(exc.getMessage(), exc);
			}
		}

		if (uElement.hasAttribute("registration")) {
			try {
				String string = uElement.getAttribute("registration");
				Date date = parseDate(string);
				user.setRegistration(date);
			} catch (ParseException exc) {
				throw new ProcessException(exc.getMessage(), exc);
			}
		}

		// TODO: parse groups and rights

		return user;
	}

}
