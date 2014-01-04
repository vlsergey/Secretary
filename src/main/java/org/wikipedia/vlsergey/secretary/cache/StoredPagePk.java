package org.wikipedia.vlsergey.secretary.cache;

import java.io.Serializable;
import java.util.Locale;

public class StoredPagePk implements Serializable {

	private static final long serialVersionUID = 1L;

	private String lang;

	private Long pageId;

	public StoredPagePk() {
	}

	public StoredPagePk(Locale locale, Long pageId) {
		this.lang = locale.getLanguage();
		this.pageId = pageId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StoredPagePk other = (StoredPagePk) obj;
		if (lang == null) {
			if (other.lang != null)
				return false;
		} else if (!lang.equals(other.lang))
			return false;
		if (pageId == null) {
			if (other.pageId != null)
				return false;
		} else if (!pageId.equals(other.pageId))
			return false;
		return true;
	}

	public String getLang() {
		return lang;
	}

	public Long getPageId() {
		return pageId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lang == null) ? 0 : lang.hashCode());
		result = prime * result + ((pageId == null) ? 0 : pageId.hashCode());
		return result;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public void setPageId(Long pageId) {
		this.pageId = pageId;
	}

	@Override
	public String toString() {
		return "PageKey [" + lang + "; " + pageId + "]";
	}

}
