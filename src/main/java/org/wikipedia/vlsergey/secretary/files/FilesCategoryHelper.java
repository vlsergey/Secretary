package org.wikipedia.vlsergey.secretary.files;

import java.util.SortedSet;
import java.util.TreeSet;

import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.functions.IteratorUtils;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMember;
import org.wikipedia.vlsergey.secretary.jwpf.model.CategoryMemberType;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

// @Component
public class FilesCategoryHelper implements Runnable {

	private MediaWikiBot mediaWikiBot;

	private WikiCache wikiCache;

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	public WikiCache getWikiCache() {
		return wikiCache;
	}

	private void outputList(SortedSet<Page> pages, String title) {
		StringBuilder stringBuilder = new StringBuilder();
		for (Page page : pages) {
			stringBuilder.append("* [[:" + page.getTitle() + "]]\n");
		}

		mediaWikiBot.writeContent("Участник:Secretary/Files/" + title, null, stringBuilder.toString(), null, "update",
				false, false);
	}

	@Override
	public void run() {
		SortedSet<Page> albums = new TreeSet<Page>();
		SortedSet<Page> cadr = new TreeSet<Page>();
		SortedSet<Page> disk = new TreeSet<Page>();
		SortedSet<Page> game = new TreeSet<Page>();
		SortedSet<Page> screenshot = new TreeSet<Page>();

		for (Revision revision : wikiCache.queryLatestContentByPageIds(IteratorUtils.map(mediaWikiBot
				.queryCategoryMembers("Категория:Файлы:Несвободные", CategoryMemberType.FILE, Namespace.FILE),
				CategoryMember.pageIdF))) {
			if (revision.getContent().contains("альбом")) {
				albums.add(revision.getPage());
			}
			if (revision.getContent().contains("кадр")) {
				cadr.add(revision.getPage());
			}
			if (revision.getContent().contains("диск")) {
				disk.add(revision.getPage());
			}
			if (revision.getContent().contains("игр")) {
				game.add(revision.getPage());
			}
			if (revision.getContent().contains("скриншот") || revision.getContent().contains("экран")) {
				screenshot.add(revision.getPage());
			}
		}

		outputList(albums, "Альбом");
		outputList(cadr, "Кадр");
		outputList(disk, "Диск");
		outputList(game, "Игра");
		outputList(screenshot, "Скриншот");
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setWikiCache(WikiCache wikiCache) {
		this.wikiCache = wikiCache;
	}
}
