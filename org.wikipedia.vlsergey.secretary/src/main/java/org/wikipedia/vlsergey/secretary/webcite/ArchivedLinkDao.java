package org.wikipedia.vlsergey.secretary.webcite;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public class ArchivedLinkDao {
	protected HibernateTemplate template = null;

	private static final Logger logger = LoggerFactory
			.getLogger(ArchivedLinkDao.class);

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<ArchivedLink> getArchivedLinks(String url, String accessDate) {
		return template.find("SELECT links " + "FROM ArchivedLink links "
				+ "WHERE accessUrl=? AND accessDate=?",
				StringUtils.trimToEmpty(url),
				StringUtils.trimToEmpty(accessDate));
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void persist(ArchivedLink archivedLink) {

		logger.info("Remember link '" + archivedLink.getArchiveUrl()
				+ "' as archived at '" + archivedLink.getArchiveDate()
				+ "' version of '" + archivedLink.getAccessUrl()
				+ "' accessed at '" + archivedLink.getAccessDate() + "'");

		template.persist(archivedLink);
	}
}
