package org.wikipedia.vlsergey.secretary.webcite;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.apache.commons.lang.StringUtils;

@Entity
public class QueuedLink {

	private String accessDate = StringUtils.EMPTY;

	private String articleDate = StringUtils.EMPTY;

	private String author = StringUtils.EMPTY;

	private long id;

	private long queuedTimestamp;

	private String title = StringUtils.EMPTY;

	private String url = StringUtils.EMPTY;

	public String getAccessDate() {
		return accessDate;
	}

	public String getArticleDate() {
		return articleDate;
	}

	@Column(length = (1 << 14) - 2)
	public String getAuthor() {
		return author;
	}

	@Id()
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}

	public long getQueuedTimestamp() {
		return queuedTimestamp;
	}

	@Column(length = (1 << 14) - 2)
	public String getTitle() {
		return title;
	}

	@Column(length = (1 << 14) - 2)
	public String getUrl() {
		return url;
	}

	public void setAccessDate(String accessDate) {
		this.accessDate = accessDate;
	}

	public void setArticleDate(String articleDate) {
		this.articleDate = articleDate;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setQueuedTimestamp(long queuedTimestamp) {
		this.queuedTimestamp = queuedTimestamp;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
