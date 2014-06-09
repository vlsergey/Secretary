package org.wikipedia.vlsergey.secretary.cache;

import java.io.Serializable;

import org.wikipedia.vlsergey.secretary.jwpf.model.Project;

public class StoredPagePk implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long pageId;

	private String project;

	public StoredPagePk() {
	}

	public StoredPagePk(Project project, Long pageId) {
		this.project = project.getCode();
		this.pageId = pageId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StoredPagePk other = (StoredPagePk) obj;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project))
			return false;
		if (pageId == null) {
			if (other.pageId != null)
				return false;
		} else if (!pageId.equals(other.pageId))
			return false;
		return true;
	}

	public Long getPageId() {
		return pageId;
	}

	public String getProject() {
		return project;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		result = prime * result + ((pageId == null) ? 0 : pageId.hashCode());
		return result;
	}

	public void setPageId(Long pageId) {
		this.pageId = pageId;
	}

	public void setProject(String lang) {
		this.project = lang;
	}

	@Override
	public String toString() {
		return "PageKey [" + project + "; " + pageId + "]";
	}

}
