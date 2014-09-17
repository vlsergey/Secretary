package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;

public class MoveDataReport {

	private final Map<EntityId, SortedMap<String, String>> results = new HashMap<>();

	public void addLine(Revision revision, ReconsiliationColumn descriptor, Collection<? extends Snak> fromWikipedia,
			Collection<? extends Snak> fromWikidata, ApiEntity entity) {
		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("| [[" + revision.getPage().getTitle() + "]]\n");
		toString(fromWikipedia, stringBuilder);
		stringBuilder.append("\n| \n");
		toString(fromWikidata, stringBuilder);
		stringBuilder.append("\n| [[:d:" + entity.getId() + "|" + entity.getId() + "]]\n");
		stringBuilder.append("|-\n");

		addLine(revision, descriptor, stringBuilder.toString());
	}

	private void addLine(Revision revision, ReconsiliationColumn descriptor, final String line) {
		synchronized (this) {
			if (!results.containsKey(descriptor.property)) {
				results.put(descriptor.property, new TreeMap<>());
			}
			results.get(descriptor.property).put(revision.getPage().getTitle(), line);
		}
	}

	public synchronized void addLine(Revision revision, ReconsiliationColumn descriptor, String problem,
			ApiEntity entity) {
		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("| [[" + revision.getPage().getTitle() + "]]\n");
		stringBuilder.append("| colspan=3 | " + problem + "\n");
		stringBuilder.append("| [[:d:" + entity.getId() + "|" + entity.getId() + "]]\n");
		stringBuilder.append("|-\n");
		addLine(revision, descriptor, stringBuilder.toString());
	}

	public synchronized void addLine(Revision revision, ReconsiliationColumn descriptor, UnsupportedParameterValue exc) {
		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("| [[" + revision.getPage().getTitle() + "]]\n");
		stringBuilder.append("| <nowiki>" + exc.getTemplatePartValue().toWiki(true) + "</nowiki>\n");
		stringBuilder.append("| colspan=3 | Can't parse value: <nowiki>" + exc.getUnparsedValue() + "</nowiki>\n");
		stringBuilder.append("|-\n");
		addLine(revision, descriptor, stringBuilder.toString());
	}

	public void save(String template, MediaWikiBot ruWikipediaBot, ReconsiliationColumn... columns) {
		for (ReconsiliationColumn column : columns) {
			// for (Map.Entry<EntityId, SortedMap<String, String>> entry :
			// results.entrySet()) {
			SortedMap<String, String> discrepancies = results.get(column.property);
			if (discrepancies != null && !discrepancies.isEmpty()) {
				StringBuilder result = new StringBuilder("{| class=\"wikitable sortable\"\n");
				result.append("! Статья\n");
				result.append("! Локальное значение\n");
				result.append("! Статус\n");
				result.append("! Значение на Викиданных\n");
				result.append("! Элемент Викиданных\n");
				result.append("|-\n");
				for (Map.Entry<String, String> line : discrepancies.entrySet()) {
					result.append(line.getValue());
				}
				result.append("|}");

				ruWikipediaBot.writeContent("User:" + ruWikipediaBot.getLogin() + "/" + template + "/P"
						+ column.property.getId(), null, result.toString(), null, "Update reconsiliation report", true,
						false);
				ruWikipediaBot.writeContent("User:" + ruWikipediaBot.getLogin() + "/" + template + "/P"
						+ column.property.getId() + "/count", null, "" + discrepancies.size(), null,
						"Update reconsiliation report", true, false);
			} else {
				ruWikipediaBot.writeContent("User:" + ruWikipediaBot.getLogin() + "/" + template + "/P"
						+ column.property.getId(), null, "(empty)", null, "Update reconsiliation report", true, false);
				ruWikipediaBot.writeContent("User:" + ruWikipediaBot.getLogin() + "/" + template + "/P"
						+ column.property.getId() + "/count", null, "" + 0, null, "Update reconsiliation report", true,
						false);
			}
		}
	}

	private void toString(Collection<? extends Snak> snaks, final StringBuilder result) {
		result.append("| ");
		if (snaks.size() == 1) {
			final Snak snak = snaks.iterator().next();
			result.append(toString(snak));
		} else {
			for (Snak snak : snaks) {
				result.append("\n* " + toString(snak));
			}
		}
	}

	private String toString(Snak snak) {
		return snak.getSnakType() == SnakType.value ? snak.getDataValue().toWiki().toWiki(true) : "("
				+ snak.getSnakType() + ")";
	}

}
