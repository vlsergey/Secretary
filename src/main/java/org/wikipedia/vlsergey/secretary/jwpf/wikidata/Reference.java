package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public interface Reference {

	String getHash();

	Snak[] getSnaks();

	Snak[] getSnaks(EntityId propertyId);

	void setHash(String str);
}
