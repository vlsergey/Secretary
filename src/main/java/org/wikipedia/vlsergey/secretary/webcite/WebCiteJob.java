package org.wikipedia.vlsergey.secretary.webcite;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
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
	private WebCiteArchiver webCiteArchiver;

	@Autowired
	private WebCiteErrorCleanup webCiteErrorCleanup;

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	@PostConstruct
	public void init() throws Exception {
		webCiteArchiver.updateIgnoringList();
		queuedLinkProcessor.start();
	}

	@Override
	public void run() {

		// webCiteErrorCleanup.errorCleanup("http://news.euro-coins.info/");
		queuedPageProcessor.clearQueue();
		queuedLinkProcessor.clearQueue();

		for (Long pageId : IteratorUtils.map(mediaWikiBot.queryCategoryMembers(
				"Категория:Википедия:Пресса о Википедии:Архив", CategoryMemberType.PAGE, Namespaces.PROJECT),
				CategoryMember.pageIdF)) {
			queuedPageDao.addPageToQueue(pageId, 5000, 0);
		}

		for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Избранная статья", Namespaces.MAIN)) {
			queuedPageDao.addPageToQueue(pageId, 1000, pageId.longValue());
		}
		for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Хорошая статья", Namespaces.MAIN)) {
			queuedPageDao.addPageToQueue(pageId, 500, pageId.longValue());
		}
		for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Избранный список или портал", Namespaces.MAIN)) {
			queuedPageDao.addPageToQueue(pageId, 500, pageId.longValue());
		}
		for (Long pageId : mediaWikiBot.queryEmbeddedInPageIds("Шаблон:Cite web", Namespaces.MAIN)) {
			queuedPageDao.addPageToQueue(pageId, 0, pageId.longValue());
		}

		//
		queuedPageProcessor.run();
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

}
