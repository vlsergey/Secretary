package org.wikipedia.vlsergey.secretary.cache;

import java.io.Serializable;
import java.util.Locale;

public class StoredRevisionPk implements Serializable {

	private static final long serialVersionUID = 1L;

	private String lang;

	private Long revisionId;

	public StoredRevisionPk() {
	}

	public StoredRevisionPk(Locale locale, Long revisionId) {
		this.lang = locale.getLanguage();
		this.revisionId = revisionId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StoredRevisionPk other = (StoredRevisionPk) obj;
		if (lang == null) {
			if (other.lang != null)
				return false;
		} else if (!lang.equals(other.lang))
			return false;
		if (revisionId == null) {
			if (other.revisionId != null)
				return false;
		} else if (!revisionId.equals(other.revisionId))
			return false;
		return true;
	}

	public String getLang() {
		return lang;
	}

	public Long getRevisionId() {
		return revisionId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((lang == null) ? 0 : lang.hashCode());
		result = prime * result + ((revisionId == null) ? 0 : revisionId.hashCode());
		return result;
	}

	public void setLang(String lang) {
		this.lang = lang;
	}

	public void setRevisionId(Long revisionId) {
		this.revisionId = revisionId;
	}

	@Override
	public String toString() {
		return "RevisionKey [" + lang + "; " + revisionId + "]";
	}

}
