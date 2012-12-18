package org.wikipedia.vlsergey.secretary.webcite;

import org.wikipedia.vlsergey.secretary.dom.Template;

class ArticleLink {

	public String accessDate;

	public String archiveDate;

	public String archiveUrl;

	public String articleDate;

	public String author;

	public Template template;

	public String title;

	public String url;

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ArticleLink other = (ArticleLink) obj;
		if (accessDate == null) {
			if (other.accessDate != null)
				return false;
		} else if (!accessDate.equals(other.accessDate))
			return false;
		if (url == null) {
			if (other.url != null)
				return false;
		} else if (!url.equals(other.url))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accessDate == null) ? 0 : accessDate.hashCode());
		result = prime * result + ((url == null) ? 0 : url.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return url + " (at " + accessDate + ")";
	}

}
