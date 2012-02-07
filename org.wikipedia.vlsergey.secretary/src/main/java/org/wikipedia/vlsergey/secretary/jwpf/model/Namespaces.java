package org.wikipedia.vlsergey.secretary.jwpf.model;

public interface Namespaces {
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
	static final int FILE = 6;

	/**
	 * This is a talk namespace that is normally used for discussions related to
	 * the associated media files. It has no special properties.
	 */
	static final int FILE_TALK = 7;

	/**
	 * Namespace zero is the 'null' namespace, commonly called the
	 * "main namespace" or "mainspace". This namespace typically contains the
	 * bulk of the content pages in a wiki. This namespace generally has no
	 * special properties.
	 */
	static final int MAIN = 0;

	/**
	 * This namespace is normally used for meta-discussions related to the
	 * operation and development of the wiki. It has no special properties.
	 */
	static final int PROJECT = 4;

	/**
	 * This is a talk namespace that is normally used for discussions related to
	 * the associated subject pages. It has no special properties.
	 */
	static final int PROJECT_TALK = 5;

	/**
	 * The "Talk" namespace is the discussion namespace attached to the
	 * mainspace. It has no special properties.
	 */
	static final int TALK = 1;
}
