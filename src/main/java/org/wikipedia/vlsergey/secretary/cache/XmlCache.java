package org.wikipedia.vlsergey.secretary.cache;

import java.security.MessageDigest;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.actions.ExpandTemplates;
import org.wikipedia.vlsergey.secretary.jwpf.actions.ExpandTemplates.Prop;
import org.wikipedia.vlsergey.secretary.jwpf.model.Project;

@Component
public class XmlCache {

	private static final Log log = LogFactory.getLog(XmlCache.class);

	private static final int SEGMENTS = 64;

	private final Lock[] locks = new Lock[SEGMENTS];

	private MediaWikiBot mediaWikiBot;

	private Project project;

	protected HibernateTemplate template = null;

	public XmlCache() {
		for (int i = 0; i < SEGMENTS; i++) {
			locks[i] = new ReentrantLock();
		}
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public Project getProject() {
		return project;
	}

	public String getXml(String content) throws Exception {
		if (StringUtils.isBlank(content) || StringUtils.isWhitespace(content))
			return "<root>" + content + "</root>";

		MessageDigest md = MessageDigest.getInstance("SHA-512");
		byte[] digest = md.digest(content.getBytes("utf-8"));
		String hash = Hex.encodeHexString(digest);

		final int segmentIndex = (hash.hashCode() % SEGMENTS + SEGMENTS) % SEGMENTS;
		final Lock lock = locks[segmentIndex];

		lock.lock();
		try {

			String key = getProject().getCode() + "-" + hash;
			XmlCacheItem cacheItem = template.get(XmlCacheItem.class, key);
			if (cacheItem != null) {
				log.trace("XML for string with hash '" + key + "' found");
				return cacheItem.getXml();
			}

			ExpandTemplates expandTemplates = mediaWikiBot.expandTemplates(content, null, false, Prop.parsetree);
			final String xml = expandTemplates.getParsetree();
			if (StringUtils.isNotEmpty(StringUtils.trimToEmpty(xml))) {
				XmlCacheItem xmlCacheItem = new XmlCacheItem();
				xmlCacheItem.setHash(key);
				xmlCacheItem.setContent(content);
				xmlCacheItem.setXml(xml);
				template.save(xmlCacheItem);
			}

			return xml;
		} finally {
			lock.unlock();
		}
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setProject(Project project) {
		this.project = project;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}
}
