package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public enum StatementRank {

	/**
	 * Deprecated statements: for statements that are being discussed, or known
	 * to be erroneous, but still listed for the sake of completion or in order
	 * to prevent them being constantly added and removed. Deprecated statements
	 * only appear in search results if they are explicitly added or if they are
	 * selected based on their source. A footnote qualifier should usually
	 * accompany other-ranked statements.
	 */
	deprecated,

	/**
	 * Normal statements: if there are no preferred statements (or the query
	 * explicitly says to include normal statements too), these statements are
	 * returned. Historical values, like the population of a country in the
	 * past, might be here, as well as less representative sources which are
	 * still considered relevant.
	 */
	normal,

	/**
	 * Preferred statements: if preferred statements exist, these statements are
	 * returned in response to a query. They would, e.g., for a population
	 * contain the most recent one as long as it is regarded as sufficiently
	 * reliable. Wikidata editors might decide to mark several statements as
	 * preferred: this may be used to indicate disagreement, reflecting the
	 * knowledge diversity on the issue, or it may be used to express the notion
	 * of actually having multiple values (in case of properties like
	 * "children").
	 */
	preferred,

	;

}
