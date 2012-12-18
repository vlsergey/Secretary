package org.wikipedia.vlsergey.secretary.dom;

public class Math extends Content {
	private final String text;

	public Math(String text) {
		this.text = text;
	}

	@Override
	public String toWiki(boolean removeComments) {
		return "<math>" + text + "</math>";
	}

}
