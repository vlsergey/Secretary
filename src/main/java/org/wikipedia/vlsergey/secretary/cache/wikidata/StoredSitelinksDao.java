package org.wikipedia.vlsergey.secretary.cache.wikidata;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.hibernate.HibernateException;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.json.JSONObject;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Sitelink;

@Repository
public class StoredSitelinksDao {

	private static final int SEGMENTS = 64;

	private final Lock[] locks = new Lock[SEGMENTS];

	protected HibernateTemplate template = null;

	public StoredSitelinksDao() {
		for (int i = 0; i < SEGMENTS; i++) {
			locks[i] = new ReentrantLock();
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public int clear() {
		return template.execute(new HibernateCallback<Integer>() {
			@Override
			public Integer doInHibernate(Session session) throws HibernateException, SQLException {
				return session.createSQLQuery("DELETE FROM sitelinks").executeUpdate();
			}
		});
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public Long findMaxRevisionByPageTitle(String site, String pageTitle) {
		try {
			Number result = template.execute(new HibernateCallback<Number>() {
				@Override
				public Number doInHibernate(Session session) throws HibernateException, SQLException {
					final SQLQuery query = session
							.createSQLQuery("SELECT MAX(storedsitelinks_revision) FROM storedsitelinks_sitelinks "
									+ "WHERE title=? AND site=?");
					query.setParameter(0, pageTitle);
					query.setParameter(1, site);
					return (Number) query.uniqueResult();
				}
			});
			if (result == null) {
				return null;
			}
			return Long.valueOf(result.longValue());
		} catch (Exception exc) {
			exc.printStackTrace();
			return null;
		}
	}

	@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
	public StoredSitelinks getByKey(Long key) {
		return template.get(StoredSitelinks.class, key);
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	@Transactional(propagation = Propagation.SUPPORTS, readOnly = false)
	public void update(Revision wikidataItem) {
		try {
			final Entity entity = new Entity(new JSONObject(wikidataItem.getContent()));
			final List<Sitelink> sitelinks = entity.getSitelinks();

			final int segmentIndex = (int) (wikidataItem.getId().longValue() % SEGMENTS);
			final Lock lock = locks[segmentIndex];
			lock.lock();
			try {
				updateImpl(wikidataItem.getPage().getId(), wikidataItem.getId(), sitelinks);
			} finally {
				lock.unlock();
			}
		} catch (Exception exc) {
			exc.printStackTrace();
		}
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public void updateImpl(long pageId, long revisionId, final List<Sitelink> sitelinks) {
		StoredSitelinks stored = getByKey(revisionId);
		if (stored != null) {
			return;
		}

		StoredSitelinks storedSitelinks = new StoredSitelinks();
		storedSitelinks.setPageId(pageId);
		storedSitelinks.setRevision(revisionId);

		Map<String, String> sitelinksMap = new HashMap<>();
		for (Sitelink sitelink : sitelinks) {
			sitelinksMap.put(sitelink.getSite(), sitelink.getTitle());
		}
		storedSitelinks.setSitelinks(sitelinksMap);

		template.persist(storedSitelinks);
	}
}
