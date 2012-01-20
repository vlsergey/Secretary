package org.wikipedia.vlsergey.secretary.webcite;

import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class WebCiteLimiter {

	private static final long PERIOD = DateUtils.MILLIS_PER_MINUTE * 70;

	private static final long REQUESTS = 90;

	protected HibernateTemplate template = null;

	public void beforeRequest(String hostCode) {
		final WebCiteQueryEvent entity = new WebCiteQueryEvent();
		entity.setHostCode(hostCode);
		entity.setTimestamp(System.currentTimeMillis());
		template.persist(entity);
	}

	public boolean isAllowed(String hostCode) {
		@SuppressWarnings("unchecked")
		List<WebCiteQueryEvent> events = template.find("SELECT events "
				+ "FROM WebCiteQueryEvent events " + "WHERE hostCode = ? "
				+ "ORDER BY timestamp DESC", hostCode);

		if (events.size() >= REQUESTS) {
			WebCiteQueryEvent last = events.get(0);
			if (System.currentTimeMillis() - last.getTimestamp() > PERIOD) {
				// cleanup and allow
				template.bulkUpdate("DELETE "
						+ "FROM WebCiteQueryEvent events "
						+ "WHERE hostCode = ?", hostCode);
				return true;
			}

			return false;
		}

		return true;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}
}
