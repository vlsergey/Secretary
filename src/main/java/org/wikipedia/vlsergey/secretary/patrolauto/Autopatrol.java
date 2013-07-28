package org.wikipedia.vlsergey.secretary.patrolauto;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.FilterRedirects;
import org.wikipedia.vlsergey.secretary.jwpf.model.Page;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.model.User;

@Component
public class Autopatrol implements Runnable {

	private MediaWikiBot mediaWikiBot;

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	@Override
	public void run() {

		Set<String> trustedUsers = new HashSet<String>();

		// for (User user : mediaWikiBot.queryAllusersByGroup("sysop"))
		// trustedUsers.add(user.getName());
		// for (User user : mediaWikiBot.queryAllusersByGroup("closer"))
		// trustedUsers.add(user.getName());
		// for (User user : mediaWikiBot.queryAllusersByGroup("editor"))
		// trustedUsers.add(user.getName());
		// for (User user : mediaWikiBot.queryAllusersByGroup("autoeditor"))
		// trustedUsers.add(user.getName());
		for (User user : mediaWikiBot.queryAllusersByGroup("bot"))
			trustedUsers.add(user.getName());

		trustedUsers.add("Al Silonov");
		trustedUsers.add("Sergey kudryavtsev");
		trustedUsers.add("Vesailok");
		trustedUsers.add("DonRumata");
		trustedUsers.add("Infovarius");
		trustedUsers.add("VPliousnine");
		trustedUsers.add("Wesha");

		for (Page page : mediaWikiBot.queryUnreviewedPages(new int[] { 100 }, FilterRedirects.ALL)) {

			Collection<Revision> allRevisions = mediaWikiBot.queryRevisionsByPageId(page.getId(), null,
					Direction.NEWER, RevisionPropery.IDS, RevisionPropery.USER);

			Set<String> trustIn = new LinkedHashSet<String>();
			Revision toPatrol = null;
			for (Revision revision : allRevisions) {
				final String editor = revision.getUser();
				if (trustedUsers.contains(editor)) {
					toPatrol = revision;
					trustIn.add(editor);
				} else {
					break;
				}
			}

			if (toPatrol != null) {
				mediaWikiBot.review(toPatrol, "Autoreview. Trust in: " + trustIn, null);
			}
		}
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

}
