package org.wikipedia.vlsergey.secretary.trust;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.AbstractDaoLock;

@Component
public class RevisionAuthorshipDaoLock extends AbstractDaoLock {

	@Autowired
	private RevisionAuthorshipDao revisionAuthorshipDao;

	public RevisionAuthorship findByRevision(Project project, Long newRevisionId) {
		return withLock(newRevisionId, () -> revisionAuthorshipDao.findByRevision(project, newRevisionId));
	}

	public void store(Project project, Revision newRevision, TextChunkList authorship) {
		withLock(newRevision.getId(), () -> revisionAuthorshipDao.store(project, newRevision, authorship));
	}

}
