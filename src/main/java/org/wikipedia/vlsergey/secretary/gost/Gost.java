package org.wikipedia.vlsergey.secretary.gost;

import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.http.BasicResponseHandler;
import org.wikipedia.vlsergey.secretary.http.HttpManager;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.ExternalUrl;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

// @Component
public class Gost {
	private static abstract class BadSite {
		abstract String getDefaultEncoding();

		abstract String getName(String content);

		abstract String getUrlTemplate();

		boolean reset() {
			return false;
		}
	}

	private static final BadSite[] BAD_SITES = new BadSite[] {

	new BadSite() {

		@Override
		public String getDefaultEncoding() {
			return "Windows-1251";
		}

		@Override
		public String getName(String content) {
			return StringUtils.substringBetween(content,
					"<tr id=c2><td align=right width=250px><b>Обозначение:</b></td><td>", "</td></tr>");
		}

		@Override
		public String getUrlTemplate() {
			return "www.gostrf.com/standart/";
		}
	},

	new BadSite() {

		@Override
		public String getDefaultEncoding() {
			return "utf-8";
		}

		@Override
		public String getName(String content) {
			return StringUtils.substringBetween(content, "<h1>", "</h1>");
		}

		@Override
		public String getUrlTemplate() {
			return "*.vsegost.com";
		}
	},

	new BadSite() {

		@Override
		public String getDefaultEncoding() {
			return "utf-8";
		}

		@Override
		public String getName(String content) {
			return StringUtils.substringBetween(content, "<h1>", "</h1>");
		}

		@Override
		public String getUrlTemplate() {
			return "www.complexdoc.ru/lib";
		}
	},

	new BadSite() {

		@Override
		public String getDefaultEncoding() {
			return "Windows-1251";
		}

		@Override
		public String getName(String content) {
			return StringUtils.substringBetween(content, "<h1>", "</h1><!--noindex-->");
		}

		@Override
		public String getUrlTemplate() {
			return "www.gosthelp.ru/gost/";
		}
	},

	new BadSite() {

		@Override
		public String getDefaultEncoding() {
			return "Windows-1251";
		}

		@Override
		public String getName(String content) {
			return StringUtils.substringBetween(content,
					"<tr id=c2><td align=right width=250px><b>Обозначение:</b></td><td>", "</td></tr>");
		}

		@Override
		public String getUrlTemplate() {
			return "teksert-ntb.gubkin.ru/gost/";
		}
	},

	};

	private static final Logger logger = LoggerFactory.getLogger(Gost.class);

	@Autowired
	private BadLinkDao badLinkDao;

	@Autowired
	private HttpManager httpManager;

	private long lastHttpTime = 0;

	
	private MediaWikiBot mediaWikiBot;

	@Autowired
	private ProtectGostRuEntryDao protectGostRuEntryDao;

	
	private WikiCache wikiCache;

	private void collectBadLinks() throws Exception {

		for (BadSite badSite : BAD_SITES) {
			collectBadLinks(badSite);
		}
	}

	private void collectBadLinks(BadSite badSite) throws Exception {
		for (ExternalUrl externalUrl : mediaWikiBot.queryExternalUrlUsage("http", badSite.getUrlTemplate(), 0)) {
			final String badUrl = externalUrl.getUrl();
			try {
				BadLink badLink = badLinkDao.findByUrl(badUrl);
				if (badLink == null || StringUtils.isEmpty(badLink.getContent()) || badSite.reset()) {

					pauseIfRequired();

					HttpGet get = new HttpGet(badUrl);
					String content = httpManager.executeFromLocalhost(get,
							new BasicResponseHandler(badSite.getDefaultEncoding()));
					lastHttpTime = System.currentTimeMillis();

					BadLink newBadLink = new BadLink();
					newBadLink.setContent(content);
					newBadLink.setUrl(badUrl);
					badLink = badLinkDao.store(newBadLink);
				}

				String content = badLink.getContent();
				String name = badSite.getName(content);

				if (StringUtils.isEmpty(name)) {
					logger.warn("Unable to determ name of '" + badUrl + "' from content:\n" + content);
				}

				name = ProtectGostRuEntryDao.normalizeName(name);
				logger.debug("Name of '" + badUrl + "' determined as '" + name + "'");
				badLink.setName(name);
				badLink = badLinkDao.store(badLink);

			} catch (Exception exc) {
				logger.error("Unable to process URL '" + badUrl + "': " + exc, exc);
			}
		}
	}

	private void collectGoodLinks() throws Exception {
		for (int i = 128330; i < 179097; i++) {
			String goodUrl = "http://protect.gost.ru/document.aspx?control=7&id=" + i;

			ProtectGostRuEntry goodLink = protectGostRuEntryDao.findById(goodUrl);
			if (goodLink == null || StringUtils.isEmpty(goodLink.getContent())) {

				pauseIfRequired();

				HttpGet get = new HttpGet(goodUrl);
				String content;
				try {
					content = httpManager.executeFromLocalhost(get, new BasicResponseHandler("utf-8"));
				} catch (ClientProtocolException exc) {
					logger.warn("Unable to GET URL '" + goodUrl + "': " + exc);
					continue;
				} finally {
					lastHttpTime = System.currentTimeMillis();
				}

				ProtectGostRuEntry newGoodLink = new ProtectGostRuEntry();
				newGoodLink.setContent(content);
				newGoodLink.setUrl(goodUrl);
				goodLink = protectGostRuEntryDao.store(newGoodLink);
			}

			String content = goodLink.getContent();

			String name;
			if (content.contains("<div style=\"margin-left: 20px; margin-top: 5px;\">Найдено: ")) {
				name = "N/A";
			} else {
				name = StringUtils.substringBetween(content, "<h1 style=\"font-weight: bold; font-size: 20px;\">",
						"</h1>");
			}

			if (StringUtils.isEmpty(name)) {
				logger.warn("Unable to determ name of '" + goodUrl + "' from content:\n" + content);
			}

			name = StringUtils.replace(name, "-", "—");
			logger.debug("Name of '" + goodUrl + "' determined as '" + name + "'");

			goodLink.setName(name);

			String description = StringUtils.substringBetween(content, "<h2><b>", "</b></h2>");
			if (StringUtils.isNotEmpty(description)) {
				goodLink.setDescription(description);
			}

			goodLink = protectGostRuEntryDao.store(goodLink);
		}
	}

	private void fixBadLinks() {
		for (BadSite badSite : BAD_SITES) {
			fixBadLinks(badSite);
		}
	}

	private void fixBadLinks(final BadSite badSite) {
		for (ExternalUrl externalUrl : mediaWikiBot.queryExternalUrlUsage("http", badSite.getUrlTemplate(), 0)) {
			Revision revision = wikiCache.queryLatestRevision(externalUrl.getPageId());
			String content = revision.getContent();

			for (BadLink badLink : badLinkDao.findAll()) {

				if (!content.contains(badLink.getUrl()))
					continue;

				List<ProtectGostRuEntry> goodLinks = protectGostRuEntryDao.findByName(badLink.getName());
				if (goodLinks.isEmpty()) {
					continue;
				}
				if (goodLinks.size() > 1) {
					continue;
				}

				ProtectGostRuEntry toReplaceWith = goodLinks.get(0);
				content = StringUtils.replace(content, badLink.getUrl(), toReplaceWith.getUrl());
			}

			if (!StringUtils.equals(content, revision.getContent())) {
				mediaWikiBot.writeContent(revision, content, "Replace " + badSite.getUrlTemplate()
						+ " with protect.gost.ru", true);
			}
		}
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	private void pauseIfRequired() throws InterruptedException {
		if (System.currentTimeMillis() - lastHttpTime < 100) {
			Thread.sleep(200);
		}
	}

	public void run() throws Exception {
		// collectGoodLinks();
		collectBadLinks();
		fixBadLinks();
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}
}
