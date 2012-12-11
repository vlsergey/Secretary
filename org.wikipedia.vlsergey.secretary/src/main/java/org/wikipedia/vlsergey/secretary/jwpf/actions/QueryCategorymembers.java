package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMember;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMemberImpl;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class QueryCategorymembers extends AbstractQueryAction implements MultiAction<CategoryMember> {

	private final String categoryTitle;

	private final String namespaces;

	private String nextOffset = null;

	private List<CategoryMember> result;

	private final String type;

	public QueryCategorymembers(boolean bot, String categoryTitle, String namespaces, String type) {
		this(bot, categoryTitle, namespaces, type, null);
	}

	private QueryCategorymembers(boolean bot, String categoryTitle, String namespaces, String type, String offset) {
		super(bot);

		this.categoryTitle = categoryTitle;
		this.namespaces = namespaces;
		this.type = type;

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();

		setParameter(multipartEntity, "format", "xml");
		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "list", "categorymembers");

		setParameter(multipartEntity, "cmprop", "ids");
		setParameter(multipartEntity, "cmtitle", categoryTitle);
		setParameter(multipartEntity, "cmnamespace", namespaces);
		setParameter(multipartEntity, "cmtype", type);

		if (offset != null)
			setParameter(multipartEntity, "cmcontinue", offset);

		if (isBot()) {
			setParameter(multipartEntity, "cmlimit", "5000");
		} else {
			setParameter(multipartEntity, "cmlimit", "500");
		}

		postMethod.setEntity(multipartEntity);
		msgs.add(postMethod);
	}

	@Override
	public MultiAction<CategoryMember> getNextAction() {
		if (nextOffset == null)
			return null;

		return new QueryCategorymembers(bot, categoryTitle, namespaces, type, nextOffset);
	}

	@Override
	public Collection<CategoryMember> getResults() {
		return result;
	}

	private CategoryMember parseCategoryMember(Element cmElement) {
		CategoryMemberImpl categoryMember = new CategoryMemberImpl();

		if (cmElement.hasAttribute("ns"))
			categoryMember.setNamespace(new Integer(cmElement.getAttribute("ns")));

		if (cmElement.hasAttribute("pageid"))
			categoryMember.setPageId(new Long(cmElement.getAttribute("pageid")));

		if (cmElement.hasAttribute("title"))
			categoryMember.setPageTitle(cmElement.getAttribute("title"));

		return categoryMember;
	}

	@Override
	protected void parseQueryContinue(Element queryContinueElement) {
		Element exturlusage = (Element) queryContinueElement.getElementsByTagName("categorymembers").item(0);
		if (exturlusage != null)
			this.nextOffset = exturlusage.getAttribute("cmcontinue");
		else
			this.nextOffset = null;
	}

	@Override
	protected void parseQueryElement(Element queryElement) throws ProcessException {
		final ListAdapter<Element> cmElements = new ListAdapter<Element>(queryElement.getElementsByTagName("cm"));
		final List<CategoryMember> result = new ArrayList<CategoryMember>(cmElements.size());

		for (Element cmElement : cmElements) {
			CategoryMember cm = parseCategoryMember(cmElement);
			result.add(cm);
		}

		this.result = result;
	}

}
