package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.List;

public abstract class Section extends AbstractContainer {

	private static final long serialVersionUID = 1L;

	private Header header;

	public Section(Header header) {
		super();
		this.header = header;
	}

	@Override
	public List<? extends Content> getChildren() {
		List<Content> result = new ArrayList<Content>();
		addToChildren(result, header);
		addToChildren(result, getContent());
		return result;
	}

	public abstract ArticleFragment getContent();

	public Header getHeader() {
		return header;
	}

	public void setHeader(Header header) {
		this.header = header;
	}

}
