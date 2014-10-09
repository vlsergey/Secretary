package org.wikipedia.vlsergey.secretary.wikidata;

public class NoWikidataElementException extends UnsupportedParameterValueException {

	private static final long serialVersionUID = 1L;

	public NoWikidataElementException(String unparsedValue) {
		super("Can't find Wikidata element: ", unparsedValue);
	}

	public NoWikidataElementException(String unparsedValue, Exception exc) {
		super("Can't find Wikidata element: ", unparsedValue, exc);
	}
}
