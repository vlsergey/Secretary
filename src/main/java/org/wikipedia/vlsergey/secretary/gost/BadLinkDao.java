package org.wikipedia.vlsergey.secretary.gost;

import java.util.List;

import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

@Repository
@Transactional(readOnly = false)
public class BadLinkDao {
	private static final Logger logger = LoggerFactory.getLogger(BadLinkDao.class);

	protected HibernateTemplate template = null;

	public List<BadLink> findAll() {
		return (List) template.find("SELECT links FROM BadLink links");
	}

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public BadLink findByUrl(String url) {
		return template.get(BadLink.class, url);
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public BadLink store(BadLink entry) {
		BadLink prev = findByUrl(entry.getUrl());
		if (prev == null) {
			entry.setName(ProtectGostRuEntryDao.normalizeName(entry.getName()));
			template.save(entry);
			return findByUrl(entry.getUrl());
		}

		prev.setBinaryContent(entry.getBinaryContent());
		prev.setName(ProtectGostRuEntryDao.normalizeName(entry.getName()));
		prev.setUrl(StringUtils.trimToEmpty(entry.getUrl()));
		template.save(prev);
		return prev;
	}
}
