package org.wikipedia.vlsergey.secretary.webcite;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class WebCiteQueryEvent {
	private String hostCode;

	private Long id;

	private long timestamp;

	public String getHostCode() {
		return hostCode;
	}

	@Id()
	@Column(nullable = false)
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	public Long getId() {
		return id;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setHostCode(String hostCode) {
		this.hostCode = hostCode;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
}
