package org.wikipedia.vlsergey.secretary.wikidata;

public class CantParseValueException extends UnsupportedParameterValueException {

	private static final long serialVersionUID = 1L;

	public CantParseValueException(String unparsedValue) {
		super("Can't parse value", unparsedValue);
	}

	public CantParseValueException(String unparsedValue, Exception exc) {
		super("Can't parse value", unparsedValue, exc);
	}
}
