package org.wikipedia.vlsergey.secretary.webcite;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.utils.DateNormalizer;

public class ArchivedLinkDao {
	private static final Logger log = LoggerFactory.getLogger(ArchivedLinkDao.class);

	@Autowired
	private DateNormalizer dateNormalizer;

	protected HibernateTemplate template = null;

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<ArchivedLink> findByAccessUrl(String url) {
		return template.find("SELECT links " + "FROM ArchivedLink links " + "WHERE accessUrl=?",
				StringUtils.trimToEmpty(url));
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<ArchivedLink> findByArchiveResult(String archiveResult) {
		return template.find("SELECT links " + "FROM ArchivedLink links " + "WHERE archiveresult=?",
				StringUtils.trimToEmpty(archiveResult));
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public List<ArchivedLink> findByArchiveUrl(String url) {
		return template.find("SELECT links " + "FROM ArchivedLink links " + "WHERE archiveUrl=?",
				StringUtils.trimToEmpty(url));
	}

	@SuppressWarnings("unchecked")
	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public ArchivedLink findNonBrokenLink(String accessUrl, String accessDate) {
		accessDate = dateNormalizer.normalizeDate(StringUtils.trimToEmpty(accessDate));

		List<ArchivedLink> result = template.find("SELECT links " + "FROM ArchivedLink links "
				+ "WHERE accessUrl=? AND accessDate=? AND archiveResult!=?", StringUtils.trimToEmpty(accessUrl),
				accessDate, StringUtils.trimToEmpty(ArchivedLink.STATUS_BROKEN));

		if (result.size() > 2) {
			log.error("More than one non-broken URLs with same access date (" + accessUrl + ") and URL '" + accessDate
					+ "'");
			throw new IllegalStateException();
		}

		if (result.isEmpty())
			return null;

		return result.get(0);
	}

	@PostConstruct
	public void init() {
		normalizeDates();
	}

	@Transactional(readOnly = false, propagation = Propagation.REQUIRED)
	protected void normalizeDates() {
		log.info("Normalize links in ArchivedLinks table...");

		{
			@SuppressWarnings("unchecked")
			List<String> toNormalize = template.find("SELECT DISTINCT accessDate " + "FROM ArchivedLink links "
					+ "WHERE NOT (accessDate='') AND NOT (accessDate LIKE '"
					+ DateNormalizer.NORMALIZED_DATE_LIKE_TEMPLATE + "')");
			for (String nonNormilizedDate : toNormalize) {
				String normalized = dateNormalizer.normalizeDate(nonNormilizedDate);
				if (!StringUtils.equals(normalized, nonNormilizedDate)) {
					log.info("Normilizing access date '" + nonNormilizedDate + "' => '" + normalized + "'");
					template.bulkUpdate("UPDATE ArchivedLink SET accessDate=? WHERE accessDate=?", normalized,
							nonNormilizedDate);
				}
			}
		}

		{
			@SuppressWarnings("unchecked")
			List<String> toNormalize = template.find("SELECT DISTINCT archiveDate " + "FROM ArchivedLink links "
					+ "WHERE NOT (archiveDate='') AND NOT (archiveDate LIKE '"
					+ DateNormalizer.NORMALIZED_DATE_LIKE_TEMPLATE + "')");
			for (String nonNormilizedDate : toNormalize) {
				String normalized = dateNormalizer.normalizeDate(nonNormilizedDate);
				if (!StringUtils.equals(normalized, nonNormilizedDate)) {
					log.info("Normilizing archive date '" + nonNormilizedDate + "' => '" + normalized + "'");
					template.bulkUpdate("UPDATE ArchivedLink SET archiveDate=? WHERE archiveDate=?", normalized,
							nonNormilizedDate);
				}
			}
		}

	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void persist(ArchivedLink archivedLink) {

		archivedLink.setAccessDate(dateNormalizer.normalizeDate(StringUtils.trimToEmpty(archivedLink.getAccessDate())));
		archivedLink.setAccessUrl(StringUtils.trimToEmpty(archivedLink.getAccessUrl()));
		archivedLink
				.setArchiveDate(dateNormalizer.normalizeDate(StringUtils.trimToEmpty(archivedLink.getArchiveDate())));
		archivedLink.setArchiveResult(StringUtils.trimToEmpty(archivedLink.getArchiveResult()));
		archivedLink.setArchiveUrl(StringUtils.trimToEmpty(archivedLink.getArchiveUrl()));

		log.info("Remember link '" + archivedLink.getArchiveUrl() + "' as archived at '"
				+ archivedLink.getArchiveDate() + "' version of '" + archivedLink.getAccessUrl() + "' accessed at '"
				+ archivedLink.getAccessDate() + "'");

		template.persist(archivedLink);
	}

	public void removeByAccessUrlPrefix(String baseUrl) {
		log.info("Removing records of access URL started with '" + baseUrl + "'");
		int removed = template.bulkUpdate("DELETE FROM ArchivedLink WHERE SUBSTR(accessUrl, 1, " + baseUrl.length()
				+ ") = ?", baseUrl);
		log.info("Removed " + removed + " records");
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void setArchiveResult(ArchivedLink archivedLink, String archiveResult) {
		ArchivedLink link = template.get(ArchivedLink.class, Long.valueOf(archivedLink.getId()));
		link.setArchiveResult(StringUtils.trimToEmpty(archiveResult));
		template.persist(link);
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

}
