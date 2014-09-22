package org.wikipedia.vlsergey.secretary.wikidata;

public class AutocategorizationRequiredException extends UnsupportedParameterValueException {

	private static final long serialVersionUID = 1L;

	public AutocategorizationRequiredException(String unparsedValue) {
		super("No autocategory present", unparsedValue);
	}

	public AutocategorizationRequiredException(String unparsedValue, Exception exc) {
		super("No autocategory present", unparsedValue, exc);
	}
}
