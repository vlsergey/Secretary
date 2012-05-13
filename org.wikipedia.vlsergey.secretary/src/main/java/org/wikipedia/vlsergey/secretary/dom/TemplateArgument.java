package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.List;

public class TemplateArgument extends AbstractContainer {

	private static final long serialVersionUID = 1L;

	private List<TemplatePart> parts;

	private Content title;

	public TemplateArgument(final Content templateName) {
		this.title = templateName;
		this.parts = new ArrayList<TemplatePart>(0);
	}

	public TemplateArgument(final Content templateName, List<TemplatePart> parts) {
		this.title = templateName;
		this.parts = new ArrayList<TemplatePart>(parts);
	}

	public String getCanonicalName() {
		if (title == null)
			return null;

		return title.toWiki(true).trim().toLowerCase();
	}

	@Override
	public List<Content> getChildren() {
		List<Content> result = new ArrayList<Content>();

		addToChildren(result, title);
		addToChildren(result, parts);

		return result;
	}

	public Content getName() {
		return title;
	}

	public TemplatePart getParameter(int index) {
		return parts.get(index);
	}

	public List<TemplatePart> getParameters() {
		return parts;
	}

	public void setParts(List<TemplatePart> parts) {
		this.parts = parts;
	}

	public void setTitle(Content name) {
		this.title = name;
	}

	@Override
	public String toWiki(boolean removeComments) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("{{{");
		stringBuilder.append(super.toWiki(removeComments));
		stringBuilder.append("}}}");

		return stringBuilder.toString();
	}

}
