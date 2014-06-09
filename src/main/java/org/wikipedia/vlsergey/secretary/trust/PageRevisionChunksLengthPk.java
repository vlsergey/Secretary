package org.wikipedia.vlsergey.secretary.trust;

import java.io.Serializable;

import org.wikipedia.vlsergey.secretary.jwpf.model.Project;

public class PageRevisionChunksLengthPk implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long pageId;

	private String project;

	public PageRevisionChunksLengthPk() {
	}

	public PageRevisionChunksLengthPk(Project project, Long pageId) {
		this.project = project.getCode();
		this.pageId = pageId;
	}

	public Long getPageId() {
		return pageId;
	}

	public String getProject() {
		return project;
	}

	public void setPageId(Long pageId) {
		this.pageId = pageId;
	}

	public void setProject(String lang) {
		this.project = lang;
	}

}
