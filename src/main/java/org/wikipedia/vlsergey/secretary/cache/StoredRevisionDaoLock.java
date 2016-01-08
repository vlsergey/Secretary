package org.wikipedia.vlsergey.secretary.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Component
public class StoredRevisionDaoLock {

	private static final int SEGMENTS = 64;

	private final Lock[] locks = new Lock[SEGMENTS];

	@Autowired
	private StoredRevisionDao storedRevisionDao;

	public StoredRevisionDaoLock() {
		for (int i = 0; i < SEGMENTS; i++) {
			locks[i] = new ReentrantLock();
		}
	}

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

	public <T> T withLock(long id, Callable<T> action) {
		final int segmentIndex = (int) (id % SEGMENTS);
		final Lock lock = locks[segmentIndex];
		lock.lock();
		try {
			return action.call();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

}
