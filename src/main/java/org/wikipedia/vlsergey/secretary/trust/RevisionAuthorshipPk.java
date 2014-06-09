package org.wikipedia.vlsergey.secretary.trust;

import java.io.Serializable;

import org.wikipedia.vlsergey.secretary.jwpf.model.Project;

public class RevisionAuthorshipPk implements Serializable {

	private static final long serialVersionUID = 1L;

	private String project;

	private Long revisionId;

	public RevisionAuthorshipPk() {
	}

	public RevisionAuthorshipPk(Project project, Long revisionId) {
		this.project = project.getCode();
		this.revisionId = revisionId;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RevisionAuthorshipPk other = (RevisionAuthorshipPk) obj;
		if (project == null) {
			if (other.project != null)
				return false;
		} else if (!project.equals(other.project))
			return false;
		if (revisionId == null) {
			if (other.revisionId != null)
				return false;
		} else if (!revisionId.equals(other.revisionId))
			return false;
		return true;
	}

	public String getProject() {
		return project;
	}

	public Long getRevisionId() {
		return revisionId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((project == null) ? 0 : project.hashCode());
		result = prime * result + ((revisionId == null) ? 0 : revisionId.hashCode());
		return result;
	}

	public void setProject(String lang) {
		this.project = lang;
	}

	public void setRevisionId(Long revisionId) {
		this.revisionId = revisionId;
	}

}
