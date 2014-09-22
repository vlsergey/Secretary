package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public interface Statement {

	public static final EntityId PROPERTY_DATE_RETRIEVED = EntityId.property(813);
	public static final EntityId PROPERTY_IMPORTED_FROM = EntityId.property(143);

	void addQualifier(ApiSnak qualifier);

	String getId();

	Snak getMainSnak();

	Snak[] getQualifiers();

	Snak[] getQualifiers(EntityId property);

	Rank getRank();

	Reference[] getReferences();

	default StringValue getStringValue() {
		return getMainSnak().getStringValue();
	}

	default ValueType getValueType() {
		return getMainSnak().getValueType();
	}

	boolean hasMainSnak();

	default boolean hasRealReferences() {
		// has any references except ones with "obtained from"
		for (Reference reference : getReferences()) {
			for (Snak snak : reference.getSnaks()) {
				if (PROPERTY_IMPORTED_FROM.equals(snak.getProperty())
						|| PROPERTY_DATE_RETRIEVED.equals(snak.getProperty())) {
					continue;
				}
				return true;
			}
		}
		return false;
	}

	default boolean hasValue() {
		return hasMainSnak() && getMainSnak().getSnakType() == SnakType.value;
	}

	/**
	 * @return <tt>true</tt> if and only if the statement has single reference
	 *         and it is "imported from" specified place
	 */
	default boolean isImportedFrom(EntityId entityId) {
		Reference[] references = getReferences();
		if (references.length != 1) {
			return false;
		}
		Snak[] snaks = references[0].getSnaks();
		WikibaseEntityIdValue toCompare = new WikibaseEntityIdValue(entityId);

		switch (snaks.length) {
		case 1:
			return PROPERTY_IMPORTED_FROM.equals(snaks[0].getProperty()) && snaks[0].getSnakType() == SnakType.value
					&& toCompare.equals(snaks[0].getWikibaseEntityIdValue());
		case 2:
			return PROPERTY_IMPORTED_FROM.equals(snaks[0].getProperty()) && snaks[0].getSnakType() == SnakType.value
					&& toCompare.equals(snaks[0].getWikibaseEntityIdValue())
					&& PROPERTY_DATE_RETRIEVED.equals(snaks[1].getProperty())

					|| PROPERTY_IMPORTED_FROM.equals(snaks[1].getProperty())
					&& snaks[1].getSnakType() == SnakType.value
					&& toCompare.equals(snaks[1].getWikibaseEntityIdValue())
					&& PROPERTY_DATE_RETRIEVED.equals(snaks[0].getProperty());
		}
		return false;
	}

	default boolean isWikibaseEntityIdValue(EntityId entityId) {
		if (hasValue() && getMainSnak().getAbstractDataValue().getType() == ValueType.WIKIBASE_ENTITYID) {
			return entityId.equals(getMainSnak().getWikibaseEntityIdValue().getEntityId());
		}
		return false;
	}

	void setMainSnak(ApiSnak mainSnak);

	void setRank(Rank rank);

}