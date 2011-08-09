package org.wikipedia.vlsergey.secretary.cache;

import java.util.ArrayList;

import org.hibernate.SessionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Repository
public class StoredRevisionDao {
	protected HibernateTemplate template = null;

	@Autowired
	private StoredPageDao storedPageDao;

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public StoredRevision getRevisionById(Long revisionId) {
		return template.get(StoredRevision.class, revisionId);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public StoredRevision getOrCreate(Revision withContent) {
		final Long id = withContent.getId();

		StoredRevision revisionImpl = getRevisionById(id);
		if (revisionImpl == null) {
			revisionImpl = new StoredRevision();
			revisionImpl.setId(id);
			template.persist(revisionImpl);
			revisionImpl = template.get(StoredRevision.class, id);
			template.flush();
		}

		if (withContent.getPage() != null) {
			StoredPage storedPage = storedPageDao.getOrCreate(withContent
					.getPage());
			revisionImpl.setPage(storedPage);

			if (storedPage.getRevisions() == null) {
				storedPage.setRevisions(new ArrayList<StoredRevision>());
			}
			if (!storedPage.getRevisions().contains(revisionImpl))
				storedPage.getRevisions().add(revisionImpl);

			template.flush();
		}

		if (withContent.getAnon() != null)
			revisionImpl.setAnon(withContent.getAnon());
		if (withContent.getBot() != null)
			revisionImpl.setBot(withContent.getBot());
		if (withContent.getComment() != null)
			revisionImpl.setComment(withContent.getComment());
		if (withContent.getContent() != null)
			revisionImpl.setContent(withContent.getContent());
		if (withContent.getMinor() != null)
			revisionImpl.setMinor(withContent.getMinor());
		if (withContent.getSize() != null)
			revisionImpl.setSize(withContent.getSize());
		if (withContent.getTimestamp() != null)
			revisionImpl.setTimestamp(withContent.getTimestamp());
		if (withContent.getUser() != null)
			revisionImpl.setUser(withContent.getUser());

		template.flush();
		return revisionImpl;
	}
}
