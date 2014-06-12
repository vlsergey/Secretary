package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.beans.PropertyEditorSupport;

public class ProjectEditor extends PropertyEditorSupport {

	@Override
	public void setAsText(String text) throws IllegalArgumentException {

		if (text.equals("ruwiki") || text.equals("ruwukipedia")) {
			setValue(Project.RUWIKIPEDIA);
			return;
		}
		if (text.equals("ukwiki") || text.equals("ukwukipedia")) {
			setValue(Project.UKWIKIPEDIA);
			return;
		}

		if (text.equals("ruwikisource")) {
			setValue(Project.RUWIKISOURCE);
			return;
		}
		if (text.equals("ukwikisource")) {
			setValue(Project.UKWIKISOURCE);
			return;
		}

		if (text.equals("wikidata")) {
			setValue(Project.WIKIDATA);
			return;
		}

		throw new IllegalArgumentException("Unknown value: " + text);
	}
}
