package org.wikipedia.vlsergey.secretary.wikidata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;

@Component
public class EnumerateProperties implements Runnable {

	private static final Log log = LogFactory.getLog(EnumerateProperties.class);

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	@Override
	public void run() {
		StringBuilder stringBuilder = new StringBuilder("{| class=\"wikitable sortable\"\n");
		stringBuilder.append("! Page ID\n");
		stringBuilder.append("! Link \n");
		stringBuilder.append("! English Label\n");
		stringBuilder.append("! English Description\n");
		stringBuilder.append("! Russian Label\n");
		stringBuilder.append("! Russian Description\n");
		stringBuilder.append("|-\n");

		for (Revision revision : wikidataCache.queryByAllPages(Namespace.WIKIDATA_PROPERTY)) {
			try {
				JSONObject jsonObject = new JSONObject(revision.getContent());
				Entity apiEntity = new Entity(jsonObject);

				stringBuilder.append("| " + revision.getPage().getId() + "\n");
				stringBuilder.append("| [[" + revision.getPage().getTitle() + "]]\n");
				stringBuilder.append("| " + (apiEntity.hasLabel("en") ? apiEntity.getLabel("en").getValue() : "")
						+ "\n");
				stringBuilder.append("| "
						+ (apiEntity.hasDescription("en") ? apiEntity.getDescription("en").getValue() : "") + "\n");
				stringBuilder.append("| " + (apiEntity.hasLabel("ru") ? apiEntity.getLabel("ru").getValue() : "")
						+ "\n");
				stringBuilder.append("| "
						+ (apiEntity.hasDescription("ru") ? apiEntity.getDescription("ru").getValue() : "") + "\n");
				stringBuilder.append("|-\n");
			} catch (RuntimeException exc) {
				log.error("Unable to update list because of " + revision + ": " + exc, exc);
				throw exc;
			}
		}
		stringBuilder.append("|}\n");

		final MediaWikiBot bot = wikidataCache.getMediaWikiBot();
		bot.writeContent("User:" + bot.getLogin() + "/Properties", null, stringBuilder.toString(), null,
				"Update properties list", true, false);
	}
}
