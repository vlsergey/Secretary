package org.wikipedia.vlsergey.secretary.cache.users;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.jwpf.model.ParsedUser;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;

@Repository
public class StoredUserDao {

	private static final Log log = LogFactory.getLog(StoredUserDao.class);

	protected HibernateTemplate template = null;

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public StoredUser getByKey(Project project, Long userId) {
		return template.get(StoredUser.class, new StoredUserPk(project, userId));
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = true)
	public StoredUser getByKey(StoredUserPk key) {
		return template.get(StoredUser.class, key);
	}

	@Transactional(propagation = Propagation.REQUIRED, readOnly = false)
	public StoredUser getOrCreate(Project project, ParsedUser withContent) {

		final Long userId = withContent.getUserId();
		if (userId == null)
			throw new IllegalArgumentException("Unable to store user with null ID");
		if (userId.longValue() == 0l)
			throw new IllegalArgumentException("Unable to store anonymous user");

		final StoredUserPk key = new StoredUserPk(project, userId);
		StoredUser stored = getByKey(key);
		if (stored == null) {
			stored = new StoredUser();
			stored.setKey(key);
			template.persist(stored);
			stored = template.get(StoredUser.class, key);
		}

		if (updateRequired(withContent.getEditcount(), stored.getEditcount())) {
			stored.setEditcount(withContent.getEditcount());
		}

		if (updateRequired(withContent.getName(), stored.getName())) {
			stored.setName(withContent.getName());
			log.info("Store user #" + userId + " as " + withContent.getName());
		}

		return stored;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	private boolean updateRequired(final Number newValue, final Number oldValue) {
		return newValue != null && !newValue.equals(oldValue);
	}

	private boolean updateRequired(final String newValue, final String oldValue) {
		return StringUtils.isNotEmpty(newValue) && !StringUtils.equals(oldValue, newValue);
	}

}
