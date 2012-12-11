package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.util.Date;
import java.util.List;

public class RevisionFlagged {

	private Integer level = null;

	private String levelText = null;

	private List<Integer> tagsAccuracy = null;

	private Date timestamp = null;

	private String user = null;

	public Integer getLevel() {
		return level;
	}

	public String getLevelText() {
		return levelText;
	}

	public List<Integer> getTagsAccuracy() {
		return tagsAccuracy;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public String getUser() {
		return user;
	}

	public void setLevel(Integer level) {
		this.level = level;
	}

	public void setLevelText(String levelText) {
		this.levelText = levelText;
	}

	public void setTagsAccuracy(List<Integer> tagsAccuracy) {
		this.tagsAccuracy = tagsAccuracy;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public void setUser(String user) {
		this.user = user;
	}

}
