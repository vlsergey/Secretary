package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.List;

public class TemplatePart extends AbstractContainer {

	public static final String DEFAULT_EQUALS = "=";

	private String equals;

	private Content name;

	private Content value;

	public TemplatePart(Content name, Content value) {
		this.name = name;
		this.value = value;

		this.equals = name != null ? DEFAULT_EQUALS : "";
	}

	public TemplatePart(Content name, String equals, Content value) {
		this.name = name;
		this.value = value;

		this.equals = equals;
	}

	public String getCanonicalName() {
		if (name == null)
			return null;

		return name.toWiki(true).trim().toLowerCase();
	}

	@Override
	public List<Content> getChildren() {
		List<Content> result = new ArrayList<Content>();

		addToChildren(result, name);
		addToChildren(result, value);

		return result;
	}

	public String getEquals() {
		return equals;
	}

	public Content getName() {
		return name;
	}

	public Content getValue() {
		return value;
	}

	public void setEquals(String equals) {
		this.equals = equals;
	}

	public void setName(Content name) {
		this.name = name;
	}

	public void setValue(Content value) {
		this.value = value;
	}

	@Override
	public String toWiki(boolean removeComments) {
		final StringBuilder result = new StringBuilder();
		result.append('|');
		if (name != null) {
			result.append(name.toWiki(removeComments));
		}
		if (equals != null) {
			result.append(equals);
		}
		if (value != null) {
			result.append(value.toWiki(removeComments));
		}
		return result.toString();
	}
}
