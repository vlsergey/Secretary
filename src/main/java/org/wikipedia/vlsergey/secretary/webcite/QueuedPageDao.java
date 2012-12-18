package org.wikipedia.vlsergey.secretary.webcite;

import java.sql.SQLException;
import java.util.List;

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
	public void addPageToQueue(Long pageId, long priority, long lastCheckTimestamp) {

		@SuppressWarnings("unchecked")
		List<QueuedPage> previous = template.find("SELECT page " + "FROM QueuedPage page " + "WHERE id=?", pageId);

		if (previous.size() > 1)
			throw new IllegalStateException("Too many queued pages in DB with id #" + pageId);

		if (previous.isEmpty()) {
			QueuedPage queuedPage = new QueuedPage();
			queuedPage.setId(pageId);
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
	public long findCount() {
		return ((Number) template.find("SELECT COUNT(pages) FROM QueuedPage pages").get(0)).longValue();
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public QueuedPage getPageFromQueue() {
		return template.execute(new HibernateCallback<QueuedPage>() {
			@Override
			public QueuedPage doInHibernate(Session session) throws HibernateException, SQLException {

				Query query = session.createQuery("SELECT pages " + "FROM QueuedPage pages "
						+ "ORDER BY lastCheckTimestamp, priority DESC");
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
	public List<QueuedPage> getPagesFromQueue() {
		return template.find("SELECT pages " + "FROM QueuedPage pages " + "ORDER BY lastCheckTimestamp, priority DESC");
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeAll() {
		template.bulkUpdate("DELETE FROM QueuedPage");
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
