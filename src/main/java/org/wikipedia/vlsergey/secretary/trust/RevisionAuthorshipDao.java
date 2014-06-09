package org.wikipedia.vlsergey.secretary.trust;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Repository
@Transactional(readOnly = false)
public class RevisionAuthorshipDao {

	private static final Logger log = LoggerFactory.getLogger(RevisionAuthorshipDao.class);

	protected HibernateTemplate template = null;

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public RevisionAuthorship findByKey(RevisionAuthorshipPk key) {
		return template.get(RevisionAuthorship.class, key);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public RevisionAuthorship findByRevision(Project project, Long revisionId) {
		return template.get(RevisionAuthorship.class, new RevisionAuthorshipPk(project, revisionId));
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public RevisionAuthorship findByRevision(Project project, Revision revision) {
		return template.get(RevisionAuthorship.class, new RevisionAuthorshipPk(project, revision.getId()));
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<Long> getAllRevisionIds(Project project) {
		return template.find("SELECT key.revisionId " + "FROM RevisionAuthorship revisionAuthorship "
				+ "WHERE key.project=? " + "ORDER BY id", project.getCode());
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeAll() {
		template.execute(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException, SQLException {
				return session.createSQLQuery("TRUNCATE TABLE RevisionAuthorship").executeUpdate();
			}
		});
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public synchronized void store(Project project, Revision revision, TextChunkList authorship) throws Exception {

		final byte[] data = authorship.toBinary();
		log.debug("Store " + data.length + " bytes of authorship info for rev#" + revision);

		final RevisionAuthorshipPk key = new RevisionAuthorshipPk(project, revision.getId());
		RevisionAuthorship already = findByKey(key);
		if (already != null) {
			already.setData(data);
			template.save(already);
		} else {
			RevisionAuthorship revisionAuthorship = new RevisionAuthorship();
			revisionAuthorship.setData(data);
			revisionAuthorship.setKey(key);
			template.save(revisionAuthorship);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public RevisionAuthorship store(RevisionAuthorship entry) {
		template.save(entry);
		return findByKey(entry.getKey());
	}
}
