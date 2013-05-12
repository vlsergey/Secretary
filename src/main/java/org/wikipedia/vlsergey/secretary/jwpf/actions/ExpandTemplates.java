package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.text.ParseException;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class ExpandTemplates extends AbstractAPIAction {

	private String expandtemplates;

	private String parsetree;

	/**
	 * @param bot
	 * @param text
	 *            Wikitext to convert
	 * @param title
	 *            Title of page
	 * @param generatexml
	 *            Generate XML parse tree
	 * @param includecomments
	 *            Whether to include HTML comments in the output
	 */
	public ExpandTemplates(boolean bot, String text, String title, boolean generatexml, boolean includecomments) {
		super(bot);

		log.info("expandTemplates( '"
				+ StringUtils.substring(text, 0, 32).replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n")
				+ "...' ; " + title + "; " + generatexml + "; " + includecomments + ")");

		HttpPost postMethod = new HttpPost("/api.php");

		MultipartEntity multipartEntity = new MultipartEntity();
		setParameter(multipartEntity, "action", "expandtemplates");
		setParameter(multipartEntity, "text", text);

		if (title != null)
			setParameter(multipartEntity, "title", title);

		if (generatexml)
			setParameter(multipartEntity, "generatexml", "1");

		if (includecomments)
			setParameter(multipartEntity, "includecomments", "1");

		setParameter(multipartEntity, "format", "xml");

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	public String getExpandtemplates() {
		return expandtemplates;
	}

	public String getParsetree() {
		return parsetree;
	}

	@Override
	protected void parseAPI(Element root) throws ProcessException, ParseException {
		{
			final ListAdapter<Element> parsetreeElements = new ListAdapter<Element>(
					root.getElementsByTagName("parsetree"));
			if (!parsetreeElements.isEmpty()) {
				if (parsetreeElements.size() > 1)
					throw new IllegalArgumentException("Too many parsetree elements: " + parsetreeElements.size());
				this.parsetree = getText(parsetreeElements.get(0));
			}
		}

		{
			final ListAdapter<Element> expandtemplatesElements = new ListAdapter<Element>(
					root.getElementsByTagName("expandtemplates"));
			if (!expandtemplatesElements.isEmpty()) {
				if (expandtemplatesElements.size() > 1)
					throw new IllegalArgumentException("Too many expandtemplates elements: "
							+ expandtemplatesElements.size());
				this.expandtemplates = getText(expandtemplatesElements.get(0));
			}
		}
	}
}
