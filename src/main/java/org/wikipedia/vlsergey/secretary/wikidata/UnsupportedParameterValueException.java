package org.wikipedia.vlsergey.secretary.wikidata;

import org.wikipedia.vlsergey.secretary.dom.Content;

public class UnsupportedParameterValueException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	private Content templatePartValue;

	private final String unparsedValue;

	protected UnsupportedParameterValueException(String reason, String unparsedValue) {
		super(reason + ": «" + unparsedValue + "»");
		this.unparsedValue = unparsedValue;
	}

	protected UnsupportedParameterValueException(String reason, String unparsedValue, Exception exc) {
		super(reason + ": «" + unparsedValue + "»: " + exc, exc);
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
