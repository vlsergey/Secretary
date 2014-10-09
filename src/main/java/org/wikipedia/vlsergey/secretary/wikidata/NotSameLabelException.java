package org.wikipedia.vlsergey.secretary.wikidata;

public class NotSameLabelException extends UnsupportedParameterValueException {

	private static final long serialVersionUID = 1L;

	protected NotSameLabelException(String wikidataValue, String wikipediaValue) {
		super("Not same label " + wikidataValue + " != ", wikipediaValue);
	}

}
