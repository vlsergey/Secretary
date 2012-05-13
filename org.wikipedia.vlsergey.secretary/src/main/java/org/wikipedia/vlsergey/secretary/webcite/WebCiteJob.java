package org.wikipedia.vlsergey.secretary.webcite;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.functions.IteratorUtils;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMember;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMemberType;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespaces;

@Component
public class WebCiteJob implements Runnable {

	private MediaWikiBot mediaWikiBot;

	@Autowired
	private QueuedLinkProcessor queuedLinkProcessor;

	@Autowired
	private QueuedPageDao queuedPageDao;

	@Autowired
	private QueuedPageProcessor queuedPageProcessor;

	@Autowired
	private TaskScheduler taskScheduler;

	@Autowired
	private WebCiteArchiver webCiteArchiver;

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	@PostConstruct
	public void init() throws Exception {
		webCiteArchiver.updateIgnoringList();
		queuedLinkProcessor.start();

		taskScheduler.scheduleWithFixedDelay(this, DateUtils.MILLIS_PER_DAY);
	}

	@Override
	public void run() {

		for (Long pageId : IteratorUtils.map(mediaWikiBot.queryCategoryMembers(
				"Категория:Википедия:Пресса о Википедии:Архив", CategoryMemberType.PAGE, Namespaces.PROJECT),
				CategoryMember.pageIdF)) {
			queuedPageDao.addPageToQueue(pageId, 5000, 0);
		}

		for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Избранная статья")) {
			queuedPageDao.addPageToQueue(pageId, 1000, pageId.longValue());
		}
		for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Хорошая статья")) {
			queuedPageDao.addPageToQueue(pageId, 500, pageId.longValue());
		}
		for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Избранный список или портал")) {
			queuedPageDao.addPageToQueue(pageId, 500, pageId.longValue());
		}
		for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Cite web")) {
			queuedPageDao.addPageToQueue(pageId, 0, pageId.longValue());
		}

		// queuedLinkProcessor.clearQueue();
		queuedPageProcessor.run();
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

}
