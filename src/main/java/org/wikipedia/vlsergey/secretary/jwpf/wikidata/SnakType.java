package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public enum SnakType {

	/**
	 * Is a marker for when there certainly is no value for the property
	 * (example: if a human has no children, the corresponding item would
	 * receive this marker for child (P40)). Assigning the "no value" marker is
	 * a proper statement and is different to an item lacking a property. Latter
	 * implicates that it is unknown whether the property has no or some value
	 * (example: a missing human that may be dead or alive cannot be assigned
	 * death date (P570) while, for consistency, a living human should feature
	 * death date (P570) with the no value marker applied, clearly denoting that
	 * the human is not dead).
	 * 
	 * (from Wikidata Help)
	 */
	novalue,

	/**
	 * Is a marker for when there is some value but the exact value is not known
	 * for the property. "Some value" means that there is nothing known for the
	 * value except that it should exist and not imply a negation of the claim
	 * (example: if the date of a human's death is completely unknown the item
	 * would receive this marker for death date (P570), denoting that the human
	 * is, in fact, dead — however, with the date of death being unknown).
	 * 
	 * (from Wikidata Help)
	 */
	somevalue,

	/**
	 * Is a marker for when there is a known value for the property that can be
	 * specified. This is the default snak type when creating a
	 * snak/claim/statement.
	 * 
	 * (from Wikidata Help)
	 */
	value,

	;

}
