package org.wikipedia.vlsergey.secretary.qarticles;

import java.util.regex.Pattern;

import org.apache.commons.lang3.time.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Section;
import org.wikipedia.vlsergey.secretary.dom.parser.XmlParser;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Component
public class QualityArticles implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(QualityArticles.class);

	private static final String NOMINATIONS_PAGE = "Википедия:Добротные статьи/Номинации";

	private static final String TEMPLATE_BOT_WARNING = "Шаблон:Предупрждение от бота";

	private MediaWikiBot mediaWikiBot;

	@Autowired
	private TaskScheduler taskScheduler;

	private WikiCache wikiCache;

	private void addWarning(Section section, String string) {
		// TODO Auto-generated method stub

	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	public void init() {
		taskScheduler.scheduleWithFixedDelay(this, DateUtils.MILLIS_PER_HOUR);
	}

	@Override
	public void run() {
		try {
			runImpl();
		} catch (Exception exc) {
			log.error("QualityArticles::run(): " + exc, exc);
		}
	}

	private void runImpl() throws Exception {
		Revision revision = wikiCache.queryLatestRevision(NOMINATIONS_PAGE);
		ArticleFragment dom = new XmlParser().parse(revision);

		for (Section section : dom.findTopLevelSections(2)) {
			String title = section.getHeader().getName();

			Pattern pattern = Pattern.compile("\\[\\[(^[\\[\\]]*)(\\|.*)?\\]\\]");
			boolean found = pattern.matcher(title).find();
			if (!found) {

				addWarning(section, "Имя секции не содержит викиссылки на статью");

			}
		}
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}
}
