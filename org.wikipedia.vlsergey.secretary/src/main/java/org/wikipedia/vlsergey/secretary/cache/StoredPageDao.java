package org.wikipedia.vlsergey.secretary.cache;

import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

@Repository
public class StoredPageDao {
	protected HibernateTemplate template = null;

	@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
	public StoredPage getById(Long pageId) {
		return template.get(StoredPage.class, pageId);
	}

	@Transactional(propagation = Propagation.MANDATORY, readOnly = false)
	public StoredPage getOrCreate(Page withContent) {
		final Long id = withContent.getId();
		// boolean flushRequired = false;

		StoredPage stored = getById(id);
		if (stored == null) {
			stored = new StoredPage();
			stored.setId(id);
			template.persist(stored);
			stored = template.get(StoredPage.class, id);
			// flushRequired = true;
		}

		if (updateRequired(withContent.getTitle(), stored.getTitle())) {
			stored.setTitle(withContent.getTitle());
			// flushRequired = true;
		}

		if (updateRequired(withContent.getNamespace(), stored.getNamespace())) {
			stored.setNamespace(withContent.getNamespace());
			// flushRequired = true;
		}

		if (updateRequired(withContent.getMissing(), stored.getMissing())) {
			stored.setMissing(withContent.getMissing());
			// flushRequired = true;
		}

		// if (flushRequired)
		// template.flush();

		return stored;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	private boolean updateRequired(final Boolean newValue,
			final Boolean oldValue) {
		return newValue != null && !newValue.equals(oldValue);
	}

	private boolean updateRequired(final Integer newValue,
			final Integer oldValue) {
		return newValue != null && !newValue.equals(oldValue);
	}

	private boolean updateRequired(final String newValue, final String oldValue) {
		return StringUtils.isNotEmpty(newValue)
				&& !StringUtils.equals(oldValue, newValue);
	}
}
