package org.wikipedia.vlsergey.secretary.tasks;

import java.util.Date;
import java.util.SortedSet;
import java.util.TreeSet;

import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Direction;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.model.RevisionPropery;
import org.wikipedia.vlsergey.secretary.jwpf.model.UserContributionItem;
import org.wikipedia.vlsergey.secretary.jwpf.model.UserContributionProperty;

@Component
public class CheckUsers implements Runnable {

	private MediaWikiBot mediaWikiBot;

	private boolean checkEdits(String userName, final Date firstDate, final Date lastDate, Namespace[] namespaces,
			int requiredEdits) {

		if (firstDate != null && firstDate.after(lastDate)) {
			throw new IllegalArgumentException();
		}

		int counter = 0;
		for (UserContributionItem item : mediaWikiBot.queryUserContributions(lastDate, firstDate, Direction.OLDER,
				userName, namespaces, UserContributionProperty.ids, UserContributionProperty.timestamp)) {
			if (firstDate != null && item.timestamp.before(firstDate)) {
				continue;
			}
			if (lastDate != null && item.timestamp.after(lastDate)) {
				continue;
			}
			counter++;
			if (counter >= requiredEdits) {
				break;
			}
		}
		boolean okay = counter >= requiredEdits;
		if (!okay) {
			System.err.println("\tNot enought edits (required " + requiredEdits + ", have " + counter + ") between "
					+ lastDate + " and " + firstDate + ": " + userName);
		}
		return okay;
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	@Override
	public void run() {

		SortedSet<String> users = new TreeSet<>();

		for (Revision revision : mediaWikiBot.queryRevisionsByPageId(5076018l, null, Direction.NEWER,
				RevisionPropery.USER)) {
			users.add(revision.getUser());
		}
		System.err.print(users.size());

		// Set<String> failed = new TreeSet<>();
		// for (String userName : users) {
		// /*
		// * не менее 100 осмысленных правок в пространстве статей раздела
		// * Википедии на русском языке до 13 июня включительно
		// */
		// boolean first = checkEdits(userName, null, new
		// Date("Sat, 14 June 2014 00:00:00 GMT"), Namespace.NSS_MAIN,
		// 100);
		// /* не менее одной правки в период между 15 апреля и 15 мая */
		// boolean second = checkEdits(userName, new
		// Date("Mon, 14 Apr 2014 23:59:59 GMT"), new Date(
		// "Fri, 16 May 2014 00:00:00 GMT"), null, 1);
		// /* не менее одной правки в период с 31 мая до 13 июня включительно */
		// boolean third = checkEdits(userName, new
		// Date("Fri, 30 May 2014 23:59:59 GMT"), new Date(
		// "Sat, 14 June 2014 00:00:00 GMT"), null, 1);
		// if (!first || !second || !third) {
		// failed.add(userName);
		// }
		//
		// }
		// System.err.print("Check failed: " + failed);
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}
}
