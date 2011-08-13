package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.List;

public class TableColumnBreak extends AbstractContainer {
	private Content attributes;

	private Content border;

	private Text tail;

	public TableColumnBreak(Content border, Content attributes, Text tail) {
		this.border = border;
		this.attributes = attributes;
		this.tail = tail;
	}

	public Content getAttributes() {
		return attributes;
	}

	public Content getBorder() {
		return border;
	}

	@Override
	public List<Content> getChildren() {
		List<Content> result = new ArrayList<Content>();

		addToChildren(result, border);
		addToChildren(result, attributes);
		addToChildren(result, tail);

		return result;
	}

	public Text getTail() {
		return tail;
	}

	public void setTail(Text tail) {
		this.tail = tail;
	}

}
