package org.wikipedia.vlsergey.secretary.webcite;

import java.sql.SQLException;
import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

@Repository
@Transactional(readOnly = false)
public class QueuedLinkDao {
	private static final Logger logger = LoggerFactory
			.getLogger(QueuedLinkDao.class);

	protected HibernateTemplate template = null;

	@Transactional(isolation = Isolation.SERIALIZABLE, readOnly = false, propagation = Propagation.REQUIRED)
	public void addLinkToQueue(QueuedLink link) {
		QueuedLink prev = findLink(link.getUrl(), link.getAccessDate());

		if (prev == null) {
			logger.info("Adding link '" + link.getUrl() + "' ('"
					+ link.getAccessDate() + "') to queue");

			link.setQueuedTimestamp(System.currentTimeMillis());
			link.setAccessDate(StringUtils.trimToEmpty(link.getAccessDate()));
			link.setArticleDate(StringUtils.trimToEmpty(link.getArticleDate()));
			link.setAuthor(StringUtils.trimToEmpty(link.getAuthor()));
			link.setTitle(StringUtils.trimToEmpty(link.getTitle()));
			link.setUrl(StringUtils.trimToEmpty(link.getUrl()));
			template.save(link);
			return;
		}

		boolean updated = false;
		if (StringUtils.isEmpty(prev.getAuthor())
				&& StringUtils.isNotEmpty(link.getAuthor())) {
			prev.setAuthor(StringUtils.trimToEmpty(link.getAuthor()));
			updated = true;
		}

		if (prev.getPriority() < link.getPriority()) {
			prev.setPriority(link.getPriority());
			updated = true;
		}

		if (StringUtils.isEmpty(prev.getTitle())
				&& StringUtils.isNotEmpty(link.getTitle())) {
			prev.setTitle(StringUtils.trimToEmpty(link.getTitle()));
			updated = true;
		}

		if (updated) {
			logger.info("Updating queued link '" + link.getUrl() + "' ('"
					+ link.getAccessDate() + "')...");

			template.flush();
		}
	}

	@SuppressWarnings("unchecked")
	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public List<QueuedLink> findByUrl(String url) {
		logger.debug("Selecting link by url and access date...");

		return template.find("SELECT link " + "FROM QueuedLink link "
				+ "WHERE url=?", StringUtils.trimToEmpty(url));
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public long findCount() {
		logger.debug("Fentching number of queued links...");

		return ((Number) template.find(
				"SELECT COUNT(links) FROM QueuedLink links").get(0))
				.longValue();
	}

	@Transactional(isolation = Isolation.SERIALIZABLE, readOnly = true, propagation = Propagation.MANDATORY)
	public QueuedLink findLink(String url, String accessedDate) {
		logger.debug("Selecting link by url and access date...");

		@SuppressWarnings("unchecked")
		List<QueuedLink> result = template.find("SELECT link "
				+ "FROM QueuedLink link " + "WHERE url=? AND accessDate=?",
				StringUtils.trimToEmpty(url),
				StringUtils.trimToEmpty(accessedDate));

		if (result.size() > 1)
			throw new IllegalStateException("Too many links in DB with url='"
					+ url + "' AND accessDate='" + accessedDate + "'");

		if (result.isEmpty())
			return null;

		return result.get(0);
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public QueuedLink getLinkFromQueue() {
		logger.debug("Selecting link to process...");

		return template.execute(new HibernateCallback<QueuedLink>() {
			@Override
			public QueuedLink doInHibernate(Session session)
					throws HibernateException, SQLException {

				Query query = session.createQuery("SELECT links "
						+ "FROM QueuedLink links "
						+ "ORDER BY priority DESC, queuedTimestamp");
				query.setMaxResults(1);

				@SuppressWarnings("unchecked")
				List<QueuedLink> result = query.list();
				if (result == null || result.isEmpty())
					return null;

				return result.get(0);
			}
		});
	}

	public boolean hasLink(String url, String accessedDate) {
		return findLink(url, accessedDate) != null;
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void reducePriority(QueuedLink queuedLink) {
		logger.info("Reducing priority of queued link '" + queuedLink.getUrl()
				+ "' ('" + queuedLink.getAccessDate() + "')...");

		template.update(queuedLink);
		final long oldPriority = queuedLink.getPriority();
		if (oldPriority > 2) {
			queuedLink.setPriority(oldPriority / 2);
		} else if (oldPriority >= 0) {
			queuedLink.setPriority(-1);
		} else {
			queuedLink.setPriority(oldPriority - 1);
		}
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public void removeLinkFromQueue(QueuedLink queuedLink) {
		logger.info("Removing link '" + queuedLink.getUrl() + "' ('"
				+ queuedLink.getAccessDate() + "') from queue...");

		template.update(queuedLink);
		template.delete(queuedLink);
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}
}
