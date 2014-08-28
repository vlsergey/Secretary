package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

import java.beans.PropertyEditorSupport;

public class EntityIdEditor extends PropertyEditorSupport {

	@Override
	public String getAsText() {
		return ((EntityId) getValue()).toString().toUpperCase();
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		setValue(EntityId.parse(text));
	}

}
