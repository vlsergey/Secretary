package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public enum ValueType {

	statement,

	string,

	time,

	wikibase_entityid {
		@Override
		public String toString() {
			return "wikibase-entityid";
		}
	}

	;

}
