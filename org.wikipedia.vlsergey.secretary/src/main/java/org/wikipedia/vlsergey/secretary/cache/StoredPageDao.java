package org.wikipedia.vlsergey.secretary.cache;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;

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

		StoredPage stored = getById(id);
		if (stored == null) {
			stored = new StoredPage();
			stored.setId(id);
			template.persist(stored);
			stored = template.get(StoredPage.class, id);
			template.flush();
		}

		if (StringUtils.isNotEmpty(withContent.getTitle())) {
			(stored).setTitle(withContent.getTitle());
		}

		if (withContent.getNamespace() != null) {
			(stored).setNamespace(withContent.getNamespace());
		}

		if (withContent.getMissing() != null) {
			(stored).setMissing(withContent.getMissing());
		}

		template.flush();
		return stored;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}
}
