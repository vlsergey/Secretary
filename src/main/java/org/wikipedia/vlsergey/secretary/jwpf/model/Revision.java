package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.util.Date;

public interface Revision {

	Boolean getAnon();

	Boolean getBot();

	String getComment();

	String getContent();

	Long getId();

	Boolean getMinor();

	Page getPage();

	Long getSize();

	Date getTimestamp();

	String getUser();

	Boolean getUserHidden();

	Long getUserId();

	UserKey getUserKey();

	String getXml();

	boolean hasContent();

	boolean hasXml();

	boolean isUserHidden();
}
