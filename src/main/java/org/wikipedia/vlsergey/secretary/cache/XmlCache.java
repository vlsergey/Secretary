package org.wikipedia.vlsergey.secretary.cache;

import java.security.MessageDigest;
import java.util.Locale;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.SessionFactory;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.actions.ExpandTemplates;

@Component
public class XmlCache {

	private static final Log log = LogFactory.getLog(XmlCache.class);

	private Locale locale;

	private MediaWikiBot mediaWikiBot;

	protected HibernateTemplate template = null;

	public Locale getLocale() {
		return locale;
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public String getXml(String content) throws Exception {
		if (StringUtils.isBlank(content) || StringUtils.isWhitespace(content))
			return "<root>" + content + "</root>";

		MessageDigest md = MessageDigest.getInstance("SHA-512");
		byte[] digest = md.digest(content.getBytes("utf-8"));
		String hash = Hex.encodeHexString(digest);

		String key = getLocale().getLanguage() + "-" + hash;
		XmlCacheItem cacheItem = template.get(XmlCacheItem.class, key);
		if (cacheItem != null) {
			log.trace("XML for string with hash '" + key + "' found");
			return cacheItem.getXml();
		}

		ExpandTemplates expandTemplates = mediaWikiBot.expandTemplates(content, null, true, false);
		final String xml = expandTemplates.getParsetree();
		if (StringUtils.isNotEmpty(StringUtils.trimToEmpty(xml))) {
			XmlCacheItem xmlCacheItem = new XmlCacheItem();
			xmlCacheItem.setHash(key);
			xmlCacheItem.setContent(content);
			xmlCacheItem.setXml(xml);
			template.save(xmlCacheItem);
		}

		return xml;
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		template = new HibernateTemplate(sessionFactory);
	}
}
