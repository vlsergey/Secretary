package org.wikipedia.vlsergey.secretary.webcite;

import java.io.Serializable;
import java.util.Locale;

import javax.persistence.Column;

public class QueuedPagePk implements Serializable {

	private static final long serialVersionUID = 1L;

	private String lang;

	private Long pageId;

	public QueuedPagePk() {
	}

	public QueuedPagePk(Locale locale, Long pageId) {
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
		QueuedPagePk other = (QueuedPagePk) obj;
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

	@Column(columnDefinition = "VARCHAR(3) WITH DEFAULT 'xx'", nullable = false, length = 3)
	public String getLang() {
		return lang;
	}

	@Column(columnDefinition = "BIGINT WITH DEFAULT -1", nullable = false)
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
		return lang + "-" + pageId;
	}

}
