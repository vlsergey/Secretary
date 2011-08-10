package org.wikipedia.vlsergey.secretary.webcite;

import java.util.List;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class WebCiteLimiter {

	private static final long PERIOD = DateUtils.MILLIS_PER_MINUTE * 70;

	private static final long REQUESTS = 90;

	protected HibernateTemplate template = null;

	public void beforeRequest() {
		final WebCiteQueryEvent entity = new WebCiteQueryEvent();
		entity.setId(System.currentTimeMillis());
		template.persist(entity);
	}

	public long getNextAllowedTime() {
		@SuppressWarnings("unchecked")
		List<WebCiteQueryEvent> events = template.find("SELECT events "
				+ "FROM WebCiteQueryEvent events " + "WHERE id > ? "
				+ "ORDER BY id DESC",
				Long.valueOf(System.currentTimeMillis() - PERIOD));

		if (events.isEmpty())
			return System.currentTimeMillis() + 1000;

		if (events.size() >= REQUESTS) {
			return events.get(0).getId() + PERIOD + 5000;
		}

		return events.get(0).getId() + 1000;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}
}
