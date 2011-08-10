package org.wikipedia.vlsergey.secretary.webcite;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class QueuedPage {

	private Long id;

	private long lastCheckTimestamp;

	@Id
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public long getLastCheckTimestamp() {
		return lastCheckTimestamp;
	}

	public void setLastCheckTimestamp(long lastCheckTimestamp) {
		this.lastCheckTimestamp = lastCheckTimestamp;
	}

}
