package org.wikipedia.vlsergey.secretary.trust;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;

@Entity
public class ToDateArticleRevision {

	private ToDateArticleRevisionPk key;

	private Long revisionId;

	@EmbeddedId
	public ToDateArticleRevisionPk getKey() {
		return key;
	}

	public Long getRevisionId() {
		return revisionId;
	}

	public void setKey(ToDateArticleRevisionPk key) {
		this.key = key;
	}

	public void setRevisionId(Long revisionId) {
		this.revisionId = revisionId;
	}
}
