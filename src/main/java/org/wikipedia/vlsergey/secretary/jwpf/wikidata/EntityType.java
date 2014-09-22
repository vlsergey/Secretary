package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public enum EntityType {

	item("Q") {
		@Override
		public String getPageTitle(long itemId) {
			return "Q" + itemId;
		}
	},

	property("P") {
		@Override
		public String getPageTitle(long itemId) {
			return "Property:P" + itemId;
		}
	},

	;

	final String code;

	private EntityType(String code) {
		this.code = code;
	}

	public abstract String getPageTitle(long itemId);

	public String toWikilink(long itemId, boolean interwiki) {
		return interwiki ? "[[:d:" + getPageTitle(itemId) + "|" + code + itemId + "]]" : "[[" + getPageTitle(itemId)
				+ "|" + code + itemId + "]]";
	}

}
