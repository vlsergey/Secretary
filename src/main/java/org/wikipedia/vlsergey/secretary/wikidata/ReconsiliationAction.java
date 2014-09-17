package org.wikipedia.vlsergey.secretary.wikidata;

enum ReconsiliationAction {

	set(true),

	remove_from_wikipedia(true),

	replace(true),

	report_difference(false),

	;

	final boolean removeFromWikipedia;

	private ReconsiliationAction(final boolean removeFromWikipedia) {
		this.removeFromWikipedia = removeFromWikipedia;
	}

}