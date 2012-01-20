package org.wikipedia.vlsergey.secretary.webcite;

import javax.persistence.Entity;
import javax.persistence.Id;

import org.wikipedia.vlsergey.secretary.functions.Function;

@Entity
public class QueuedPage {

	public static Function<QueuedPage, Long> getIdF() {
		return new Function<QueuedPage, Long>() {
			@Override
			public Long apply(QueuedPage a) {
				return a.getId();
			}
		};
	}

	private Long id;

	private long lastCheckTimestamp;

	private long priority;

	@Id
	public Long getId() {
		return id;
	}

	public long getLastCheckTimestamp() {
		return lastCheckTimestamp;
	}

	public long getPriority() {
		return priority;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setLastCheckTimestamp(long lastCheckTimestamp) {
		this.lastCheckTimestamp = lastCheckTimestamp;
	}

	public void setPriority(long priority) {
		this.priority = priority;
	}

}
