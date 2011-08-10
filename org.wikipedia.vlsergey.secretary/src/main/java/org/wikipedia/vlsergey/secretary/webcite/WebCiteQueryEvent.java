package org.wikipedia.vlsergey.secretary.webcite;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class WebCiteQueryEvent {
	private long id;

	@Id
	public long getId() {
		return id;
	}

	public void setId(long timestamp) {
		this.id = timestamp;
	}
}
