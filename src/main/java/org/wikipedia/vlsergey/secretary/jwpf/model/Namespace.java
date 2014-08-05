package org.wikipedia.vlsergey.secretary.jwpf.model;

public enum Namespace {

	/**
	 * The Category namespace contains categories, dynamic lists of other pages.
	 * To facilitate this, linking directly to a category page does not output
	 * an inline link, but instead includes the page into the associated
	 * category page. So the code [[Category:Help]] causes a category link to
	 * appear at the bottom of the page (at the bottom in the box marked
	 * "Categories"). Clicking on that link takes you to the category page,
	 * where this page is visible in the category list. To create an inline link
	 * to a category page, you need to add a colon to the front of the
	 * namespace: [[:Category:Help]] produces Category:Help. See Help:Categories
	 * for more details on category link syntax.
	 */
	CATEGORY(14),

	/**
	 * This is a talk namespace that is normally used for discussions related to
	 * the associated category pages. It has no special properties.
	 */
	CATEGORY_TALK(15),

	/**
	 * The File namespace is used to store metadata for images, videos, sound
	 * files and other media accessed via the Media namespace. Each file has a
	 * corresponding page in the File namespace which is often used to hold
	 * licensing data. Linking directly to a page in this namespace instead
	 * includes the media file inline in the page: [[File:Wiki.png|right]]
	 * produces the image to the right. See Help:Images for more details of this
	 * link syntax. To create an internal link to the file page, you need to add
	 * a colon to the front of the namespace: [[:File:Wiki.png|right]] produces
	 * File:Wiki.png. The standard MediaWiki installation has alias "Image" for
	 * File namespace - See Namespace aliases.
	 */
	FILE(6),

	/**
	 * This is a talk namespace that is normally used for discussions related to
	 * the associated media files. It has no special properties.
	 */
	FILE_TALK(7),

	/**
	 * Namespace zero is the 'null' namespace, commonly called the
	 * "main namespace" or "mainspace". This namespace typically contains the
	 * bulk of the content pages in a wiki. This namespace generally has no
	 * special properties.
	 */
	MAIN(0),

	/**
	 * This namespace is normally used for meta-discussions related to the
	 * operation and development of the wiki. It has no special properties.
	 */
	PROJECT(4),

	/**
	 * This is a talk namespace that is normally used for discussions related to
	 * the associated subject pages. It has no special properties.
	 */
	PROJECT_TALK(5),

	RU_WIKI_PORTAL(100),

	RU_WIKI_PORTAL_TALK(100),

	/**
	 * The "Talk" namespace is the discussion namespace attached to the
	 * mainspace. It has no special properties.
	 */
	TALK(1),

	/**
	 * The Template namespace is used to hold templates, blocks of text or
	 * wikicode that are intended to be transcluded into several other pages. To
	 * facilitate this it has the special property that it is the default
	 * namespace for transclusions: the wikicode {{Foo}} is equivalent to
	 * {{Template:Foo}}.
	 */
	TEMPLATE(10),

	/**
	 * This is a talk namespace that is normally used for discussions related to
	 * the associated template pages. It has no special properties.
	 */
	TEMPLATE_TALK(11),

	/**
	 * Each user has a corresponding page in the User namespace, which is linked
	 * to from edit histories, watchlists, recent changes, etc; wherever an
	 * individual user is uniquely identified. This page, and subpages of it,
	 * can be used by users to record information about themselves or to test
	 * and draft new content. Pages of the form "User:UserName/Foo.js" or
	 * "User:UserName/Foo.css" can only be edited by the user themselves or by
	 * administrators.
	 */
	USER(2),

	/**
	 * This namespace is the talkspace associated with the User namespace. Pages
	 * in this namespace are mainly used to leave messages for particular users
	 * or inform them of discussions that may be of interest to them. To
	 * facilitate this, when a page User talk:UserName is edited, whenever the
	 * user "UserName" loads a page, a notice is displayed at the top of the
	 * page informing them of the edit.
	 */
	USER_TALK(3),

	;

	public static final Namespace[] NSS_MAIN = new Namespace[] { Namespace.MAIN };

	public final int id;

	private Namespace(final int id) {
		this.id = id;
	}
}
