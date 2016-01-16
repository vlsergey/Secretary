package org.wikipedia.vlsergey.secretary.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.AbstractDaoLock;

@Component
public class StoredRevisionDaoLock extends AbstractDaoLock {

	@Autowired
	private StoredRevisionDao storedRevisionDao;

	public int clear(Project project) {
		return storedRevisionDao.clear(project);
	}

	public List<StoredRevision> getOrCreate(Project project, Iterable<? extends Revision> withContent) {
		List<StoredRevision> result = new ArrayList<StoredRevision>();
		for (Revision source : withContent) {
			result.add(getOrCreate(project, source));
		}
		return result;
	}

	public StoredRevision getOrCreate(Project project, Revision withContent) {
		return withLock(withContent.getId(), () -> storedRevisionDao.getOrCreate(project, withContent));
	}

	public StoredRevision getRevisionById(Project project, Long revisionId) {
		return withLock(revisionId, () -> storedRevisionDao.getRevisionById(project, revisionId));
	}

	public StoredRevision getRevisionById(StoredRevisionPk key) {
		return withLock(key.getRevisionId(), () -> storedRevisionDao.getRevisionById(key));
	}

	public int removePageRevisionsExcept(Project project, Long pageId, Set<Long> preserveRevisionIds) {
		return storedRevisionDao.removePageRevisionsExcept(project, pageId, preserveRevisionIds);
	}

}
