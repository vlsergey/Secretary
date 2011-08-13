package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.List;

public class Table extends AbstractContainer {
	private Content tableAttributes;

	private List<Content> tableRows;

	private Text tableTail;

	public Table(Content tableAttributes, List<Content> tableRows,
			Text tableTail) {
		this.tableAttributes = tableAttributes;
		this.tableRows = tableRows;
		this.tableTail = tableTail;
	}

	@Override
	public List<Content> getChildren() {
		List<Content> result = new ArrayList<Content>();

		addToChildren(result, new Text("{|"));
		addToChildren(result, tableAttributes);
		addToChildren(result, tableRows);
		addToChildren(result, tableTail);
		addToChildren(result, new Text("|}"));

		return result;
	}

	public Content getTableAttributes() {
		return tableAttributes;
	}

	public List<Content> getTableRows() {
		return tableRows;
	}

	public Text getTableTail() {
		return tableTail;
	}

	public void setTableAttributes(Content tableAttributes) {
		this.tableAttributes = tableAttributes;
	}

	public void setTableRows(List<Content> tableRows) {
		this.tableRows = tableRows;
	}

	public void setTableTail(Text tableTail) {
		this.tableTail = tableTail;
	}

}
