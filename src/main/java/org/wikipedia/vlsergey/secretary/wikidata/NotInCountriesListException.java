package org.wikipedia.vlsergey.secretary.wikidata;

public class NotInCountriesListException extends UnsupportedParameterValueException {

	private static final long serialVersionUID = 1L;

	protected NotInCountriesListException(String wikipediaValue) {
		super("Not in countries list", wikipediaValue);
	}

}
