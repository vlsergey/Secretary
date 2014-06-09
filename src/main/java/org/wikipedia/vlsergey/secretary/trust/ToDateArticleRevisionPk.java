package org.wikipedia.vlsergey.secretary.trust;

import java.io.Serializable;
import java.util.Date;

import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;

public class ToDateArticleRevisionPk implements Serializable {

	private static final long serialVersionUID = 1L;

	private long date;

	private Long pageId;

	private String project;

	public ToDateArticleRevisionPk() {
	}

	public ToDateArticleRevisionPk(Project project, Page page, Date date) {
		this.project = project.getCode();
		this.pageId = page.getId();
		this.date = date.getTime();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ToDateArticleRevisionPk other = (ToDateArticleRevisionPk) obj;
		if (date != other.date)
			return false;
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

	public long getDate() {
		return date;
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
		result = prime * result + (int) (date ^ (date >>> 32));
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		result = prime * result + ((pageId == null) ? 0 : pageId.hashCode());
		return result;
	}

	public void setDate(long date) {
		this.date = date;
	}

	public void setPageId(Long pageId) {
		this.pageId = pageId;
	}

	public void setProject(String lang) {
		this.project = lang;
	}

}
