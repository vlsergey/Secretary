package org.wikipedia.vlsergey.secretary.dom;

public class Math extends Content {
	private final String text;

	public Math(String text) {
		this.text = text;
	}

	@Override
	public String toWiki() {
		return "<math>" + text + "</math>";
	}

}
