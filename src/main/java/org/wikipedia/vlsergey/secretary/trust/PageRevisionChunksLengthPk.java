package org.wikipedia.vlsergey.secretary.trust;

import java.io.Serializable;
import java.util.Locale;

public class PageRevisionChunksLengthPk implements Serializable {

	private static final long serialVersionUID = 1L;

	private String lang;

	private Long pageId;

	public PageRevisionChunksLengthPk() {
	}

	public PageRevisionChunksLengthPk(Locale locale, Long pageId) {
		this.lang = locale.getLanguage();
		this.pageId = pageId;
	}

	public String getLang() {
		return lang;
	}

	public Long getPageId() {
		return pageId;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public void setPageId(Long pageId) {
		this.pageId = pageId;
	}

}
