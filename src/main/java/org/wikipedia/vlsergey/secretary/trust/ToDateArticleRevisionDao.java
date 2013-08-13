package org.wikipedia.vlsergey.secretary.trust;

import java.sql.SQLException;
import java.util.Date;
import java.util.Locale;

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
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;

@Repository
@Transactional(readOnly = false)
public class ToDateArticleRevisionDao {

	private static final Logger log = LoggerFactory.getLogger(ToDateArticleRevisionDao.class);

	protected HibernateTemplate template = null;

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public ToDateArticleRevision find(Locale locale, Page page, Date date) {
		final ToDateArticleRevision result = template.get(ToDateArticleRevision.class, new ToDateArticleRevisionPk(
				locale, page, date));
		if (log.isDebugEnabled()) {
			log.debug("Query up-to-date revision for " + page + " and date " + date + " => "
					+ (result != null ? "rev#" + result.getRevisionId() : "(not found)"));
		}
		return result;
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public ToDateArticleRevision findByKey(ToDateArticleRevisionPk key) {
		return template.get(ToDateArticleRevision.class, key);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeAll() {
		template.execute(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException, SQLException {
				return session.createSQLQuery("TRUNCATE TABLE ToDateArticleRevision").executeUpdate();
			}
		});
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public synchronized void store(Locale locale, Page page, Date date, Long revisionId) throws Exception {
		log.debug("Store rev#" + revisionId + " as up-to-date for " + page + " and date " + date);

		final ToDateArticleRevisionPk key = new ToDateArticleRevisionPk(locale, page, date);
		ToDateArticleRevision toDateArticleRevision = findByKey(key);
		if (toDateArticleRevision == null) {
			toDateArticleRevision = new ToDateArticleRevision();
			toDateArticleRevision.setKey(key);
		}
		toDateArticleRevision.setRevisionId(revisionId);
		template.save(toDateArticleRevision);
	}

}
