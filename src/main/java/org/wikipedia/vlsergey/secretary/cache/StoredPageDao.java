package org.wikipedia.vlsergey.secretary.cache;

import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

@Repository
public class StoredPageDao {

	protected HibernateTemplate template = null;

	@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
	public StoredPage getByKey(Project project, Long pageId) {
		return template.get(StoredPage.class, new StoredPagePk(project, pageId));
	}

	@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
	public StoredPage getByKey(StoredPagePk key) {
		return template.get(StoredPage.class, key);
	}

	@Transactional(propagation = Propagation.MANDATORY, readOnly = false)
	public StoredPage getOrCreate(Project project, Page withContent) {

		final StoredPagePk key = new StoredPagePk(project, withContent.getId());
		StoredPage stored = getByKey(key);
		if (stored == null) {
			stored = new StoredPage();
			stored.setKey(key);
			template.persist(stored);
			stored = template.get(StoredPage.class, key);
		}

		if (updateRequired(withContent.getTitle(), stored.getTitle())) {
			stored.setTitle(withContent.getTitle());
		}

		if (updateRequired(withContent.getNamespace(), stored.getNamespace())) {
			stored.setNamespace(withContent.getNamespace());
		}

		if (updateRequired(withContent.getMissing(), stored.getMissing())) {
			stored.setMissing(withContent.getMissing());
		}

		return stored;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	private boolean updateRequired(final Boolean newValue, final Boolean oldValue) {
		return newValue != null && !newValue.equals(oldValue);
	}

	private boolean updateRequired(final Integer newValue, final Integer oldValue) {
		return newValue != null && !newValue.equals(oldValue);
	}

	private boolean updateRequired(final String newValue, final String oldValue) {
		return StringUtils.isNotEmpty(newValue) && !StringUtils.equals(oldValue, newValue);
	}
}
