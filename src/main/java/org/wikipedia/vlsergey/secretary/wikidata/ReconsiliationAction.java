package org.wikipedia.vlsergey.secretary.wikidata;

enum ReconsiliationAction {

	remove_from_wikipedia_as_empty(true),

	remove_from_wikipedia_as_not_empty(true),

	replace(true),

	report_difference(false),

	set(true),

	;

	final boolean removeFromWikipedia;

	private ReconsiliationAction(final boolean removeFromWikipedia) {
		this.removeFromWikipedia = removeFromWikipedia;
	}

}