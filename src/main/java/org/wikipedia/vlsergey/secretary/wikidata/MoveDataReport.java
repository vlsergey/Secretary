package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;

public class MoveDataReport {

	private final Function<EntityId, String> labelResolver;

	private final Map<EntityId, SortedMap<String, String>> results = new HashMap<>();

	public MoveDataReport(final Function<EntityId, String> labelResolver) {
		this.labelResolver = labelResolver;
	}

	public void addLine(Revision revision, ReconsiliationColumn descriptor,
			Collection<ValueWithQualifiers> fromWikipedia, Collection<ValueWithQualifiers> fromWikidata,
			Entity entity) {
		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("| [[" + revision.getPage().getTitle() + "]]\n");
		toString(fromWikipedia, stringBuilder);
		stringBuilder.append("\n| \n");
		toString(fromWikidata, stringBuilder);
		stringBuilder.append("\n| [[:d:" + entity.getId() + "|" + labelResolver.apply(entity.getId()) + "]]\n");
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

	public synchronized void addLine(Revision revision, ReconsiliationColumn descriptor, String problem, Entity entity) {
		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("| [[" + revision.getPage().getTitle() + "]]\n");
		stringBuilder.append("| colspan=3 | " + problem + "\n");
		stringBuilder.append("| [[:d:" + entity.getId() + "|" + labelResolver.apply(entity.getId()) + "]]\n");
		stringBuilder.append("|-\n");
		addLine(revision, descriptor, stringBuilder.toString());
	}

	public synchronized void addLine(Revision revision, ReconsiliationColumn descriptor,
			UnsupportedParameterValueException exc) {
		final StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("| [[" + revision.getPage().getTitle() + "]]\n");
		stringBuilder.append("| <nowiki>" + exc.getTemplatePartValue().toWiki(true) + "</nowiki>\n");
		stringBuilder.append("| colspan=3 | <nowiki>" + exc.getMessage() + "</nowiki>\n");
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

				int count = 0;
				for (Map.Entry<String, String> line : discrepancies.entrySet()) {
					result.append(line.getValue());
					count++;
					if (count == 1000) {
						break;
					}
				}
				result.append("|}\n");

				if (discrepancies.size() > count) {
					result.append("Too many items, stop report");
				}

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

	private void toString(Collection<ValueWithQualifiers> values, final StringBuilder result) {
		result.append("| ");
		if (values.size() == 1) {
			final ValueWithQualifiers value = values.iterator().next();
			result.append(value.toString(labelResolver, 0));
		} else {
			for (ValueWithQualifiers value : values) {
				result.append("\n* " + value.toString(labelResolver, 1));
			}
		}
	}

}
