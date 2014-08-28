package org.wikipedia.vlsergey.secretary.wikidata;

import org.wikipedia.vlsergey.secretary.dom.Content;

public class UnsupportedParameterValue extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private Content templatePartValue;

	private final String unparsedValue;

	public UnsupportedParameterValue(String unparsedValue) {
		super("Unsupported parameter value: '" + unparsedValue + "'");
		this.unparsedValue = unparsedValue;
	}

	public UnsupportedParameterValue(String unparsedValue, Exception exc) {
		super("Unsupported parameter value: '" + unparsedValue + "': " + exc, exc);
		this.unparsedValue = unparsedValue;
	}

	public Content getTemplatePartValue() {
		return templatePartValue;
	}

	public String getUnparsedValue() {
		return unparsedValue;
	}

	public void setTemplatePartValue(Content templatePartValue) {
		this.templatePartValue = templatePartValue;
	}

}
