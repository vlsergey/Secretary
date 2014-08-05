package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.Arrays;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedPage;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;

public class QueryRevisionsByCategoryMembers extends AbstractQueryRevisionsAction implements MultiAction<ParsedPage> {

	public static enum CmType {

		file,

		page,

		subcat,

		;
	}

	private final Namespace[] gcmnamespace;

	/**
	 * Which category to enumerate (required). Must include 'Category:' prefix.
	 * Cannot be used together with cmpageid
	 */
	private final String gcmtitle;

	/**
	 * What type of category members to include. Ignored when cmsort=timestamp
	 * is set Values (separate with '|'): page, subcat, file
	 * 
	 * Default: page|subcat|file
	 */
	private final CmType gcmtype;

	private String geicontinue = null;

	private final RevisionPropery[] properties;

	public QueryRevisionsByCategoryMembers(boolean bot, String category, Namespace[] gcmnamespace, CmType type,
			RevisionPropery[] properties) {
		this(bot, category, gcmnamespace, type, properties, null);
	}

	private QueryRevisionsByCategoryMembers(boolean bot, String gcmtitle, Namespace[] gcmnamespace, CmType gcmtype,
			RevisionPropery[] properties, String gcmcontinue) {
		super(bot, properties);

		log.info("[action=query; prop=revisions; generator=categorymembers]: " + gcmtitle + "; "
				+ Arrays.toString(gcmnamespace) + "; " + gcmtype + "; " + Arrays.toString(properties) + "; "
				+ gcmcontinue);

		this.gcmtitle = gcmtitle;
		this.gcmnamespace = gcmnamespace;
		this.gcmtype = gcmtype;

		this.properties = properties;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setFormatXml(multipartEntity);

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "prop", "revisions");

		setParameter(multipartEntity, "generator", "categorymembers");
		setParameter(multipartEntity, "gcmtitle", gcmtitle);
		setParameter(multipartEntity, "gcmnamespace", gcmnamespace);
		setParameter(multipartEntity, "gcmlimit", String.valueOf(bot ? 5000 : 500));
		setParameter(multipartEntity, "gcmtype", gcmtype);
		setParameter(multipartEntity, "gcmcontinue", gcmcontinue);

		setParameter(multipartEntity, "rvprop", toStringParameters(properties));

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	@Override
	public MultiAction<ParsedPage> getNextAction() {
		if (geicontinue == null)
			return null;

		return new QueryRevisionsByCategoryMembers(isBot(), gcmtitle, gcmnamespace, gcmtype, properties, geicontinue);
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element embeddedinElement = (Element) queryContinueElement.getElementsByTagName("categorymembers").item(0);
		geicontinue = embeddedinElement.getAttribute("gcmcontinue");
	}

}
