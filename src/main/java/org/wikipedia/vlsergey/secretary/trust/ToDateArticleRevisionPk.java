package org.wikipedia.vlsergey.secretary.trust;

import java.io.Serializable;
import java.util.Date;
import java.util.Locale;

import org.wikipedia.vlsergey.secretary.jwpf.model.Page;

public class ToDateArticleRevisionPk implements Serializable {

	private static final long serialVersionUID = 1L;

	private long date;

	private String lang;

	private Long pageId;

	public ToDateArticleRevisionPk() {
	}

	public ToDateArticleRevisionPk(Locale locale, Page page, Date date) {
		this.lang = locale.getLanguage();
		this.pageId = page.getId();
		this.date = date.getTime();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ToDateArticleRevisionPk other = (ToDateArticleRevisionPk) obj;
		if (date != other.date)
			return false;
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

	public long getDate() {
		return date;
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
		result = prime * result + (int) (date ^ (date >>> 32));
		result = prime * result + ((lang == null) ? 0 : lang.hashCode());
		result = prime * result + ((pageId == null) ? 0 : pageId.hashCode());
		return result;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public void setPageId(Long pageId) {
		this.pageId = pageId;
	}

}
