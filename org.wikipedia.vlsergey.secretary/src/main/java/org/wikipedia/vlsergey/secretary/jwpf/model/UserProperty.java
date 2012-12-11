package org.wikipedia.vlsergey.secretary.jwpf.model;

public enum UserProperty {

	/**
	 * information about a current block on the user
	 */
	BLOCKINFO("blockinfo"),

	/**
	 * the edit count of the user
	 */
	EDITCOUNT("editcount"),

	/**
	 * groups that the user is in
	 */
	GROUPS("groups"),

	/**
	 * all the groups the user is automatically in
	 */
	IMPLICITGROUPS("implicitgroups"),

	/**
	 * the timestamp of when the user registered if available
	 */
	REGISTRATION("registration"),

	/**
	 * rights that the user has
	 */
	RIGHTS("rights");

	private String queryString;

	private UserProperty(String queryString) {
		this.queryString = queryString;
	}

	public String getQueryString() {
		return queryString;
	}

	@Override
	public String toString() {
		return getQueryString();
	}

}
