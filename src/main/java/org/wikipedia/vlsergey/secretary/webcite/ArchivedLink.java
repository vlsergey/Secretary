package org.wikipedia.vlsergey.secretary.webcite;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ArchivedLink {

	public static final String STATUS_BROKEN = "Invalid snapshot ID";

	public static final String STATUS_SUCCESS = "success";

	private String accessDate;

	private String accessUrl;

	private String archiveDate;

	private String archiveResult;

	private String archiveUrl;

	private long id;

	@Column(length = (1 << 14) - 2)
	public String getAccessDate() {
		return accessDate;
	}

	@Column(length = (1 << 14) - 2)
	public String getAccessUrl() {
		return accessUrl;
	}

	@Column(length = (1 << 14) - 2)
	public String getArchiveDate() {
		return archiveDate;
	}

	@Column(length = (1 << 14) - 2)
	public String getArchiveResult() {
		return archiveResult;
	}

	@Column(length = (1 << 14) - 2)
	public String getArchiveUrl() {
		return archiveUrl;
	}

	@Id()
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}

	public void setAccessDate(String accessDate) {
		this.accessDate = accessDate;
	}

	public void setAccessUrl(String accessUrl) {
		this.accessUrl = accessUrl;
	}

	public void setArchiveDate(String archiveDate) {
		this.archiveDate = archiveDate;
	}

	public void setArchiveResult(String archiveResult) {
		this.archiveResult = archiveResult;
	}

	public void setArchiveUrl(String archiveUrl) {
		this.archiveUrl = archiveUrl;
	}

	public void setId(long id) {
		this.id = id;
	}
}
