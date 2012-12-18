package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.util.Date;
import java.util.List;

public class LogItem {

	private String action;

	private String comment;

	private Integer ns;

	private Long pageID;

	private List<String> params;

	private Boolean patrolAuto;

	private Long patrolCurID;

	private Date timestamp;

	private String title;

	private String type;

	private String user;

	public String getAction() {
		return action;
	}

	public String getComment() {
		return comment;
	}

	public Integer getNs() {
		return ns;
	}

	public Long getPageID() {
		return pageID;
	}

	public List<String> getParams() {
		return params;
	}

	public Boolean getPatrolAuto() {
		return patrolAuto;
	}

	public Long getPatrolCurID() {
		return patrolCurID;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public String getTitle() {
		return title;
	}

	public String getType() {
		return type;
	}

	public String getUser() {
		return user;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setNs(Integer ns) {
		this.ns = ns;
	}

	public void setPageID(Long pageID) {
		this.pageID = pageID;
	}

	public void setParams(List<String> params) {
		this.params = params;
	}

	public void setPatrolAuto(Boolean patrolAuto) {
		this.patrolAuto = patrolAuto;
	}

	public void setPatrolCurID(Long patrolCurID) {
		this.patrolCurID = patrolCurID;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public void setType(String type) {
		this.type = type;
	}

	public void setUser(String user) {
		this.user = user;
	}

}
