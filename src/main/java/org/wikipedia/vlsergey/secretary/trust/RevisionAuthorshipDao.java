package org.wikipedia.vlsergey.secretary.trust;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Repository
@Transactional(readOnly = false)
public class RevisionAuthorshipDao {

	private static final Logger log = LoggerFactory.getLogger(RevisionAuthorshipDao.class);

	@SuppressWarnings("unchecked")
	static <T> List<T> fromBinary(byte[] bs) throws Exception {
		final ObjectInputStream objectInputStream = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(
				bs)));
		try {
			return (List<T>) objectInputStream.readObject();
		} finally {
			objectInputStream.close();
		}
	}

	static <T> byte[] toBinary(List<T> source) throws Exception {
		final ByteArrayOutputStream result = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(new GZIPOutputStream(result));
		try {
			stream.writeObject(new ArrayList<T>(source));
		} finally {
			stream.close();
		}
		return result.toByteArray();
	}

	protected HibernateTemplate template = null;

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public RevisionAuthorship findByKey(RevisionAuthorshipPk key) {
		return template.get(RevisionAuthorship.class, key);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public RevisionAuthorship findByRevision(Locale locale, Long revisionId) {
		return template.get(RevisionAuthorship.class, new RevisionAuthorshipPk(locale, revisionId));
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public RevisionAuthorship findByRevision(Locale locale, Revision revision) {
		return template.get(RevisionAuthorship.class, new RevisionAuthorshipPk(locale, revision.getId()));
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
	public synchronized void store(Locale locale, Revision revision, List<TextChunk> authorship) throws Exception {

		final byte[] data = toBinary(authorship);
		log.debug("Store " + data.length + " bytes of authorship info for rev#" + revision);

		final RevisionAuthorshipPk key = new RevisionAuthorshipPk(locale, revision.getId());
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
