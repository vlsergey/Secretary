package org.wikipedia.vlsergey.secretary.gost;

import java.util.Collections;
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
public class ProtectGostRuEntryDao {
	private static final Logger logger = LoggerFactory
			.getLogger(ProtectGostRuEntryDao.class);

	static String normalizeName(String name) {
		name = StringUtils.trimToEmpty(name);
		name = StringUtils.replace(name, "-", "—");
		return name;
	}

	protected HibernateTemplate template = null;

	@Transactional(readOnly = true, propagation = Propagation.REQUIRED)
	public ProtectGostRuEntry findById(String url) {
		return template.get(ProtectGostRuEntry.class, url);
	}

	@SuppressWarnings("unchecked")
	public List<ProtectGostRuEntry> findByName(String name) {
		name = normalizeName(name);

		if (StringUtils.isEmpty(name))
			return Collections.emptyList();

		return template.find("SELECT entries "
				+ "FROM ProtectGostRuEntry entries " + "WHERE name=?", name);
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	public ProtectGostRuEntry store(ProtectGostRuEntry entry) {
		ProtectGostRuEntry prev = findById(entry.getUrl());
		if (prev == null) {
			entry.setDescription(StringUtils.trimToEmpty(entry.getDescription()));
			entry.setName(normalizeName(entry.getName()));
			entry.setUrl(StringUtils.trimToEmpty(entry.getUrl()));
			template.save(entry);
			return findById(entry.getUrl());
		}

		prev.setDescription(StringUtils.trimToEmpty(entry.getDescription()));
		prev.setName(normalizeName(entry.getName()));
		prev.setUrl(StringUtils.trimToEmpty(entry.getUrl()));
		template.save(prev);
		return prev;
	}
}
