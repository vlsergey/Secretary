package org.wikipedia.vlsergey.secretary.webcite;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class ArchivedLink {
	private long accessDate;

	private String accessUrl;

	private long archiveDate;

	private ArchiveResult archiveResult;

	private String archiveUrl;

	private long id;

	public long getAccessDate() {
		return accessDate;
	}

	public String getAccessUrl() {
		return accessUrl;
	}

	public long getArchiveDate() {
		return archiveDate;
	}

	public ArchiveResult getArchiveResult() {
		return archiveResult;
	}

	public String getArchiveUrl() {
		return archiveUrl;
	}

	@Id()
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public long getId() {
		return id;
	}

	public void setAccessDate(long accessDate) {
		this.accessDate = accessDate;
	}

	public void setAccessUrl(String accessUrl) {
		this.accessUrl = accessUrl;
	}

	public void setArchiveDate(long archiveDate) {
		this.archiveDate = archiveDate;
	}

	public void setArchiveResult(ArchiveResult archiveResult) {
		this.archiveResult = archiveResult;
	}

	public void setArchiveUrl(String archiveUrl) {
		this.archiveUrl = archiveUrl;
	}

	public void setId(long id) {
		this.id = id;
	}
}
