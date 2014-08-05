package org.wikipedia.vlsergey.secretary.jwpf.model;

public enum UserContributionProperty {

	/** Adds the comment of the edit */
	comment,

	/** Adds flags of the edit */
	flags,

	/** Adds the page ID and revision ID */
	ids,

	/** Adds the parsed comment of the edit */
	parsedcomment,

	/** Tags patrolled edits */
	patrolled,

	/** Adds the new size of the edit */
	size,

	/** Adds the size delta of the edit against its parent */
	sizediff,

	/** Lists tags for the edit */
	tags,

	/** Adds the timestamp of the edit */
	timestamp,

	/** Adds the title and namespace ID of the page */
	title,

	;
}
