package org.wikipedia.vlsergey.secretary.webcite;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = false)
public class QueuedLinkDaoImpl {
	protected HibernateTemplate template = null;

	@Transactional(isolation = Isolation.SERIALIZABLE, readOnly = false, propagation = Propagation.REQUIRED)
	public void addLinkToQueue(QueuedLink link) {

		@SuppressWarnings("unchecked")
		List<QueuedLink> previous = template
				.find("SELECT link FROM WebCiteLink link WHERE url=? AND accessDate=?",
						link.getUrl(), link.getAccessDate());

		if (previous.size() > 1)
			throw new IllegalStateException("Too many links in DB with url='"
					+ link.getUrl() + "' AND accessDate='"
					+ link.getAccessDate() + "'");

		if (previous.isEmpty()) {
			template.save(link);
			return;
		}

		QueuedLink prev = previous.get(0);
		boolean updated = false;
		if (StringUtils.isEmpty(prev.getAuthor())
				&& StringUtils.isNotEmpty(link.getAuthor())) {
			prev.setAuthor(link.getAuthor());
			updated = true;
		}

		if (StringUtils.isEmpty(prev.getTitle())
				&& StringUtils.isNotEmpty(link.getTitle())) {
			prev.setTitle(link.getTitle());
			updated = true;
		}

		if (updated)
			template.flush();
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

}
