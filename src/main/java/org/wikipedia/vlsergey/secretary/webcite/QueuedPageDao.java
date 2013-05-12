package org.wikipedia.vlsergey.secretary.webcite;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = false)
public class QueuedPageDao {

	protected HibernateTemplate template = null;

	@Transactional(isolation = Isolation.SERIALIZABLE, readOnly = false, propagation = Propagation.REQUIRED)
	public void addPageToQueue(Locale locale, Long pageId, long priority, long lastCheckTimestamp) {

		QueuedPagePk key = new QueuedPagePk(locale, pageId);

		@SuppressWarnings("unchecked")
		List<QueuedPage> previous = template.find("SELECT page " + "FROM QueuedPage page "
				+ "WHERE lang=? AND pageId=?", key.getLang(), key.getPageId());

		if (previous.size() > 1)
			throw new IllegalStateException("Too many queued pages in DB with id #" + key);

		if (previous.isEmpty()) {
			QueuedPage queuedPage = new QueuedPage();
			queuedPage.setKey(key);
			queuedPage.setLastCheckTimestamp(lastCheckTimestamp);
			queuedPage.setPriority(priority);
			template.save(queuedPage);
			return;
		}

		QueuedPage prev = previous.get(0);
		prev.setPriority(Long.valueOf(Math.max(prev.getPriority(), priority)));
		prev.setLastCheckTimestamp(Long.valueOf(Math.max(prev.getLastCheckTimestamp(), lastCheckTimestamp)));
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public long findCount(Locale locale) {
		return ((Number) template.find("SELECT COUNT(pageId) FROM QueuedPage pages WHERE lang=?", locale.getLanguage())
				.get(0)).longValue();
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public QueuedPage getPageFromQueue(final Locale locale) {
		return template.execute(new HibernateCallback<QueuedPage>() {
			@Override
			public QueuedPage doInHibernate(Session session) throws HibernateException, SQLException {

				Query query = session.createQuery("SELECT pages " + "FROM QueuedPage pages " + "WHERE lang=? "
						+ "ORDER BY lastCheckTimestamp, priority DESC");
				query.setParameter(1, locale.getLanguage());
				query.setMaxResults(1);

				@SuppressWarnings("unchecked")
				List<QueuedPage> result = query.list();
				if (result == null || result.isEmpty())
					return null;

				return result.get(0);
			}
		});
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<QueuedPage> getPagesFromQueue(final Locale locale) {
		return template.find("SELECT pages " + "FROM QueuedPage pages " + "WHERE lang=? "
				+ "ORDER BY lastCheckTimestamp, priority DESC", locale.getLanguage());
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeAll(final Locale locale) {
		template.bulkUpdate("DELETE FROM QueuedPage " + "WHERE lang=?", locale.getLanguage());
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removePageFromQueue(QueuedPage queuedPage) {
		template.update(queuedPage);
		template.delete(queuedPage);
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}
}
