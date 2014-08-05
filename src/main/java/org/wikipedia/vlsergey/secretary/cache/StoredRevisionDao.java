package org.wikipedia.vlsergey.secretary.cache;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Repository
public class StoredRevisionDao {

	private static final int SEGMENTS = 64;

	private final Lock[] locks = new Lock[SEGMENTS];

	@Autowired
	private StoredPageDao storedPageDao;

	protected HibernateTemplate template = null;

	public StoredRevisionDao() {
		for (int i = 0; i < SEGMENTS; i++) {
			locks[i] = new ReentrantLock();
		}
	}

	@SuppressWarnings("unchecked")
	public List<Long> getAllRevisionIds() {
		return (List) template.find("SELECT revisions.id FROM Revision revisions ORDER BY id");
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public List<StoredRevision> getOrCreate(Project project, Iterable<? extends Revision> withContent) {
		List<StoredRevision> result = new ArrayList<StoredRevision>();
		for (Revision source : withContent) {
			result.add(getOrCreate(project, source));
		}
		return result;
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public StoredRevision getOrCreate(Project project, Revision withContent) {
		final int segmentIndex = (int) (withContent.getId().longValue() % SEGMENTS);
		final Lock lock = locks[segmentIndex];
		lock.lock();
		try {
			return getOrCreateImpl(project, withContent);
		} finally {
			lock.unlock();
		}
	}

	private StoredRevision getOrCreateImpl(Project project, Revision withContent) {
		final StoredRevisionPk key = new StoredRevisionPk(project, withContent.getId());
		StoredRevision revisionImpl = getRevisionById(key);
		if (revisionImpl == null) {
			revisionImpl = new StoredRevision();
			revisionImpl.setKey(key);
			template.persist(revisionImpl);
			revisionImpl = template.get(StoredRevision.class, key);
			// template.flush();
		}

		if (withContent.getPage() != null) {

			StoredPage storedPage;
			if (revisionImpl.getPage() == null) {
				storedPage = storedPageDao.getOrCreate(project, withContent.getPage());
				revisionImpl.setPage(storedPage);
			} else {
				storedPage = revisionImpl.getPage();
			}

		}

		if (updateRequired(withContent.getAnon(), revisionImpl.getAnon())) {
			revisionImpl.setAnon(withContent.getAnon());
		}
		if (updateRequired(withContent.getBot(), revisionImpl.getBot())) {
			revisionImpl.setBot(withContent.getBot());
		}
		if (updateRequired(withContent.getComment(), revisionImpl.getComment())) {
			revisionImpl.setComment(withContent.getComment());
		}
		if (withContent.getContent() != null && updateRequired(withContent.getContent(), revisionImpl.getContent())) {
			revisionImpl.setContent(withContent.getContent());
		}
		if (updateRequired(withContent.getMinor(), revisionImpl.getMinor())) {
			revisionImpl.setMinor(withContent.getMinor());
		}
		if (updateRequired(withContent.getSize(), revisionImpl.getSize())) {
			revisionImpl.setSize(withContent.getSize());
		}
		if (updateRequired(withContent.getTimestamp(), revisionImpl.getTimestamp())) {
			revisionImpl.setTimestamp(withContent.getTimestamp());
		}
		if (updateRequired(withContent.getUser(), revisionImpl.getUser())) {
			revisionImpl.setUser(withContent.getUser());
		}
		if (withContent.getXml() != null && updateRequired(withContent.getXml(), revisionImpl.getXml())) {
			revisionImpl.setXml(withContent.getXml());
		}

		return revisionImpl;
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public StoredRevision getRevisionById(Project project, Long revisionId) {
		return getRevisionById(new StoredRevisionPk(project, revisionId));
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public StoredRevision getRevisionById(StoredRevisionPk key) {
		return template.get(StoredRevision.class, key);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public int removePageRevisionsExcept(final Project project, final Long pageId, Set<Long> preserveRevisionIds) {

		final StringBuilder idsBuilder = new StringBuilder();
		for (Long id : preserveRevisionIds) {
			if (idsBuilder.length() != 0) {
				idsBuilder.append(",");
			}
			idsBuilder.append(id);
		}

		int updated = template.execute(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException, SQLException {
				final SQLQuery query = session.createSQLQuery("DELETE FROM revision "
						+ "WHERE project=? AND page_project=? AND page_pageid=? AND revisionid NOT IN ("
						+ idsBuilder.toString() + ")");
				query.setParameter(0, project.getCode());
				query.setParameter(1, project.getCode());
				query.setParameter(2, pageId);
				return query.executeUpdate();
			}
		});

		return updated;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void updateCache(Project project, Revision withContent) {
		getOrCreate(project, withContent);
	}

	private boolean updateRequired(final Object newValue, final Object oldValue) {
		return newValue != null && !newValue.equals(oldValue);
	}

}
