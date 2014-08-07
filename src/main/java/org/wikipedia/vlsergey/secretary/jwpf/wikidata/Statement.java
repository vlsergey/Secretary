package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public interface Statement {

	void addQualifier(String propertyCode, ApiSnak qualifier);

	Snak getMainSnak();

	Snak[] getQualifiers(String propertyCode);

	Rank getRank();

	default StringValue getStringValue() {
		return getMainSnak().getStringValue();
	}

	boolean hasMainSnak();

	default boolean hasValue() {
		return hasMainSnak() && getMainSnak().getSnakType() == SnakType.value;
	}

	default boolean isWikibaseEntityIdValue(String entityId) {
		if (hasMainSnak()) {
			if (getMainSnak().hasSnakType()
					&& getMainSnak().getSnakType() == SnakType.value) {
				return entityId.equals("Q"
						+ getMainSnak().getWikibaseEntityIdValue()
								.getNumericId());
			}
		}
		return false;
	}

	void setMainSnak(ApiSnak mainSnak);

	void setRank(Rank rank);

}