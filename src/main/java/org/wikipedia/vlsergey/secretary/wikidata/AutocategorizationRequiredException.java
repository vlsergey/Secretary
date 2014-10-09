package org.wikipedia.vlsergey.secretary.wikidata;

import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;

public class AutocategorizationRequiredException extends UnsupportedParameterValueException {

	private static final long serialVersionUID = 1L;

	public AutocategorizationRequiredException(String unparsedValue, EntityId entityId) {
		super("No autocategory present", unparsedValue);
		setEntityId(entityId);
	}

	public AutocategorizationRequiredException(String unparsedValue, EntityId entityId, Exception exc) {
		super("No autocategory present", unparsedValue, exc);
		setEntityId(entityId);
	}
}
