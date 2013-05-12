package org.wikipedia.vlsergey.secretary.webcite;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Transient;

import org.wikipedia.vlsergey.secretary.functions.Function;

@Entity
public class QueuedPage {

	public static Function<QueuedPage, Long> getPageIdF() {
		return new Function<QueuedPage, Long>() {
			@Override
			public Long apply(QueuedPage a) {
				return a.getKey().getPageId();
			}
		};
	}

	private QueuedPagePk key;

	private long lastCheckTimestamp;

	private long priority;

	@EmbeddedId
	public QueuedPagePk getKey() {
		return key;
	}

	@Transient
	public String getLang() {
		return key.getLang();
	}

	public long getLastCheckTimestamp() {
		return lastCheckTimestamp;
	}

	@Transient
	public Long getPageId() {
		return key.getPageId();
	}

	public long getPriority() {
		return priority;
	}

	public void setKey(QueuedPagePk key) {
		this.key = key;
	}

	public void setLastCheckTimestamp(long lastCheckTimestamp) {
		this.lastCheckTimestamp = lastCheckTimestamp;
	}

	public void setPriority(long priority) {
		this.priority = priority;
	}

}
