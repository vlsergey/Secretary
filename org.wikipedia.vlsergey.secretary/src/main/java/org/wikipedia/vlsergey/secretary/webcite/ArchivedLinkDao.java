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
	private static final Logger logger = LoggerFactory
			.getLogger(ArchivedLinkDao.class);

	protected HibernateTemplate template = null;

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public ArchivedLink findLink(String url, String accessDate) {
		List<ArchivedLink> result = template.find("SELECT links "
				+ "FROM ArchivedLink links "
				+ "WHERE accessUrl=? AND accessDate=?",
				StringUtils.trimToEmpty(url),
				StringUtils.trimToEmpty(accessDate));

		if (result.size() > 2) {
			logger.error("More than one URLs with same access date (" + url
					+ ") and URL '" + accessDate + "'");
			throw new IllegalStateException();
		}

		if (result.isEmpty())
			return null;

		return result.get(0);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void persist(ArchivedLink archivedLink) {

		archivedLink.setAccessDate(StringUtils.trimToEmpty(archivedLink
				.getAccessDate()));
		archivedLink.setAccessUrl(StringUtils.trimToEmpty(archivedLink
				.getAccessUrl()));
		archivedLink.setArchiveDate(StringUtils.trimToEmpty(archivedLink
				.getArchiveDate()));
		archivedLink.setArchiveResult(StringUtils.trimToEmpty(archivedLink
				.getArchiveResult()));
		archivedLink.setArchiveUrl(StringUtils.trimToEmpty(archivedLink
				.getArchiveUrl()));

		logger.info("Remember link '" + archivedLink.getArchiveUrl()
				+ "' as archived at '" + archivedLink.getArchiveDate()
				+ "' version of '" + archivedLink.getAccessUrl()
				+ "' accessed at '" + archivedLink.getAccessDate() + "'");

		template.persist(archivedLink);
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}
}
