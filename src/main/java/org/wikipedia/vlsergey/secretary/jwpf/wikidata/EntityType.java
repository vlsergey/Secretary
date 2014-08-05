package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public enum EntityType {

	item("Q"),

	property("P"),

	;

	final String code;

	private EntityType(String code) {
		this.code = code;
	}

}
