package org.wikipedia.vlsergey.secretary.cache.users;

import java.io.Serializable;

import org.wikipedia.vlsergey.secretary.jwpf.model.Project;

public class StoredUserPk implements Serializable {

	private static final long serialVersionUID = 1L;

	private Long userId;

	private String project;

	public StoredUserPk() {
	}

	public StoredUserPk(Project project, Long pageId) {
		this.project = project.getCode();
		this.userId = pageId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		StoredUserPk other = (StoredUserPk) obj;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project))
			return false;
		if (userId == null) {
			if (other.userId != null)
				return false;
		} else if (!userId.equals(other.userId))
			return false;
		return true;
	}

	public Long getUserId() {
		return userId;
	}

	public String getProject() {
		return project;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		result = prime * result + ((userId == null) ? 0 : userId.hashCode());
		return result;
	}

	public void setUserId(Long pageId) {
		this.userId = pageId;
	}

	public void setProject(String lang) {
		this.project = lang;
	}

	@Override
	public String toString() {
		return "UserKey [" + project + "; " + userId + "]";
	}

}
