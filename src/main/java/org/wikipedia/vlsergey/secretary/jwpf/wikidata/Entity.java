package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public interface Entity extends Comparable<Entity> {

	@Override
	default int compareTo(Entity o) {
		return this.getId().compareTo(o.getId());
	}

	Statement[] getClaims(EntityId property);

	EntityId getId();

	Sitelink getSiteLink(String code);

	boolean hasClaims(EntityId property);

	boolean hasDescription(String langCode);

	boolean hasLabel(String langCode);

	boolean hasSitelink(String projectCode);

}