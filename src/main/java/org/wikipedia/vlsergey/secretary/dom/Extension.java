package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.List;

public class Extension extends AbstractContainer {

	private static final long serialVersionUID = 1L;

	private Content attr;

	private Content close;

	private Content inner;

	private Content name;

	public Extension(Content name, Content attr, Content inner, Content close) {
		super();
		this.name = name;
		this.attr = attr;
		this.inner = inner;
		this.close = close;
	}

	@Override
	public List<? extends Content> getChildren() {
		List<Content> result = new ArrayList<Content>();

		addToChildren(result, name);
		addToChildren(result, attr);
		addToChildren(result, inner);
		addToChildren(result, close);

		return result;
	}

	public Content getClose() {
		return close;
	}

	public Content getInner() {
		return inner;
	}

	public Content getName() {
		return name;
	}

	public void setClose(Content close) {
		this.close = close;
	}

	public void setInner(Content inner) {
		this.inner = inner;
	}

	public void setName(Content name) {
		this.name = name;
	}

	@Override
	public String toWiki(boolean removeComments) {
		StringBuilder result = new StringBuilder();
		result.append('<');
		result.append(getName().toWiki(removeComments));
		if (attr != null) {
			result.append(attr.toWiki(removeComments));
		}

		if (inner == null && close == null) {
			result.append("/>");
			return result.toString();
		}

		result.append('>');
		if (inner != null) {
			result.append(inner.toWiki(removeComments));
		}
		if (close != null) {
			result.append(close.toWiki(removeComments));
		}
		return result.toString();
	}
}
