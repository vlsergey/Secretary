package org.wikipedia.vlsergey.secretary.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Repository
public class StoredRevisionDao {

	@Autowired
	private StoredPageDao storedPageDao;

	protected HibernateTemplate template = null;

	@SuppressWarnings("unchecked")
	public List<Long> getAllRevisionIds() {
		return template.find("SELECT revisions.id FROM Revision revisions ORDER BY id");
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public StoredRevision getOrCreate(Locale locale, Revision withContent) {

		final StoredRevisionPk key = new StoredRevisionPk(locale, withContent.getId());
		StoredRevision revisionImpl = getRevisionById(key);
		if (revisionImpl == null) {
			revisionImpl = new StoredRevision();
			revisionImpl.setKey(key);
			template.persist(revisionImpl);
			revisionImpl = template.get(StoredRevision.class, key);
			// template.flush();
		}

		if (withContent.getPage() != null) {
			boolean flushRequired = false;

			StoredPage storedPage;
			if (revisionImpl.getPage() == null) {
				storedPage = storedPageDao.getOrCreate(locale, withContent.getPage());
				revisionImpl.setPage(storedPage);
				flushRequired = true;
			} else {
				storedPage = revisionImpl.getPage();
			}

			if (storedPage.getRevisions() == null) {
				storedPage.setRevisions(new ArrayList<StoredRevision>());
				flushRequired = true;
			}
			if (!storedPage.getRevisions().contains(revisionImpl)) {
				storedPage.getRevisions().add(revisionImpl);
				flushRequired = true;
			}

			// if (flushRequired)
			// template.flush();
		}

		boolean flushRequired = false;

		if (updateRequired(withContent.getAnon(), revisionImpl.getAnon())) {
			revisionImpl.setAnon(withContent.getAnon());
			flushRequired = true;
		}
		if (updateRequired(withContent.getBot(), revisionImpl.getBot())) {
			revisionImpl.setBot(withContent.getBot());
			flushRequired = true;
		}
		if (updateRequired(withContent.getComment(), revisionImpl.getComment())) {
			revisionImpl.setComment(withContent.getComment());
			flushRequired = true;
		}
		if (updateRequired(withContent.getContent(), revisionImpl.getContent())) {
			revisionImpl.setContent(withContent.getContent());
			flushRequired = true;
		}
		if (updateRequired(withContent.getMinor(), revisionImpl.getMinor())) {
			revisionImpl.setMinor(withContent.getMinor());
			flushRequired = true;
		}
		if (updateRequired(withContent.getSize(), revisionImpl.getSize())) {
			revisionImpl.setSize(withContent.getSize());
			flushRequired = true;
		}
		if (updateRequired(withContent.getTimestamp(), revisionImpl.getTimestamp())) {
			revisionImpl.setTimestamp(withContent.getTimestamp());
			flushRequired = true;
		}
		if (updateRequired(withContent.getUser(), revisionImpl.getUser())) {
			revisionImpl.setUser(withContent.getUser());
			flushRequired = true;
		}
		if (updateRequired(withContent.getXml(), revisionImpl.getXml())) {
			revisionImpl.setXml(withContent.getXml());
			flushRequired = true;
		}

		// if (flushRequired)
		// template.flush();

		return revisionImpl;
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = true)
	public StoredRevision getRevisionById(Locale locale, Long revisionId) {
		return getRevisionById(new StoredRevisionPk(locale, revisionId));
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public StoredRevision getRevisionById(StoredRevisionPk key) {
		return template.get(StoredRevision.class, key);
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void updateCache(Locale locale, Revision withContent) {
		getOrCreate(locale, withContent);
	}

	private boolean updateRequired(final Object newValue, final Object oldValue) {
		return newValue != null && !newValue.equals(oldValue);
	}

}
