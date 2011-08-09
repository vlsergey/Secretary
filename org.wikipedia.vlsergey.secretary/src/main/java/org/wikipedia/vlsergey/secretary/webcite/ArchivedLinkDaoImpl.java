package org.wikipedia.vlsergey.secretary.webcite;

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;

public class ArchivedLinkDaoImpl {
	protected HibernateTemplate template = null;

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}

	@SuppressWarnings("unchecked")
	public List<ArchivedLink> getArchivedLinks(String url, long accessDate) {
		return template.find("SELECT links " + "FROM ArchivedLink links "
				+ "WHERE accessUrl=? AND accessDate=?",
				StringUtils.trimToEmpty(url), Long.valueOf(accessDate));
	}
}
