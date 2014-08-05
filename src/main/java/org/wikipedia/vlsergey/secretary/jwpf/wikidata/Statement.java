package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public interface Statement {

	void addQualifier(String propertyCode, ApiSnak qualifier);

	Snak getMainSnak();

	Snak[] getQualifiers(String propertyCode);

	Rank getRank();

	boolean hasMainSnak();

	boolean isWikibaseEntityIdValue(String entityId);

	void setMainSnak(ApiSnak mainSnak);

	void setRank(Rank rank);

}