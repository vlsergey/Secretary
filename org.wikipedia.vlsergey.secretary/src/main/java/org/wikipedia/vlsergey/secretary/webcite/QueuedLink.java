package org.wikipedia.vlsergey.secretary.webcite;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class QueuedLink {

	public static final long DATE_UNSPECIFIED = 0;

	private long accessDate = DATE_UNSPECIFIED;

	private long articleDate = DATE_UNSPECIFIED;

	private String author;

	private long id;

	private String title;

	private String url;

	public long getAccessDate() {
		return accessDate;
	}

	public long getArticleDate() {
		return articleDate;
	}

	public String getAuthor() {
		return author;
	}

	@Id()
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public String getUrl() {
		return url;
	}

	public void setAccessDate(Long accessDate) {
		this.accessDate = accessDate == null ? DATE_UNSPECIFIED : accessDate
				.longValue();
	}

	public void setArticleDate(Long articleDate) {
		this.articleDate = articleDate == null ? DATE_UNSPECIFIED : articleDate
				.longValue();
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
