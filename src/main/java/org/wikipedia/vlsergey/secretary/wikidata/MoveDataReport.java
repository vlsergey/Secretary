package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataValue;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.wikidata.MoveDataToWikidata.PropertyDescriptor;

public class MoveDataReport {

	private final Map<EntityId, StringBuilder> builders = new HashMap<>();

	public synchronized void addLine(Revision revision, PropertyDescriptor descriptor, Set<DataValue> fromWikipedia,
			Set<DataValue> fromWikidata, ApiEntity entity) {
		final StringBuilder stringBuilder = getStringBuilder(descriptor.property);

		stringBuilder.append("| [[" + revision.getPage().getTitle() + "]]\n");
		stringBuilder.append("| ");
		if (fromWikipedia.size() == 1) {
			stringBuilder.append(fromWikipedia.iterator().next().toWiki().toWiki(true));
		} else {
			for (DataValue dataValue : fromWikipedia) {
				stringBuilder.append("\n* " + dataValue.toWiki().toWiki(true));
			}
		}
		stringBuilder.append("\n| \n");
		stringBuilder.append("| ");
		if (fromWikidata.size() == 1) {
			stringBuilder.append(fromWikidata.iterator().next().toWiki().toWiki(true));
		} else {
			for (DataValue dataValue : fromWikidata) {
				stringBuilder.append("\n* " + dataValue.toWiki().toWiki(true));
			}
		}
		stringBuilder.append("\n| [[:d:" + entity.getId() + "|" + entity.getId() + "]]\n");
		stringBuilder.append("|-\n");
	}

	public synchronized void addLine(Revision revision, PropertyDescriptor descriptor, UnsupportedParameterValue exc) {
		final StringBuilder stringBuilder = getStringBuilder(descriptor.property);

		stringBuilder.append("| [[" + revision.getPage().getTitle() + "]]\n");
		stringBuilder.append("| <nowiki>" + exc.getTemplatePartValue().toWiki(true) + "</nowiki>\n");
		stringBuilder.append("| Can't parse value: <nowiki>" + exc.getUnparsedValue() + "</nowiki>\n");
		stringBuilder.append("|\n");
		stringBuilder.append("|\n");
		stringBuilder.append("|-\n");
	}

	private synchronized StringBuilder getStringBuilder(EntityId property) {
		StringBuilder result = builders.get(property);
		if (result == null) {
			result = new StringBuilder("{| class=\"wikitable sortable\"\n");
			result.append("! Статья\n");
			result.append("! Локальное значение\n");
			result.append("! Статус\n");
			result.append("! Значение на Викиданных\n");
			result.append("! Элемент Викиданных\n");
			result.append("|-\n");
			builders.put(property, result);
		}
		return result;
	}

	public void save(String template, MediaWikiBot ruWikipediaBot) {
		for (Map.Entry<EntityId, StringBuilder> entry : builders.entrySet()) {
			entry.getValue().append("|}");
			ruWikipediaBot.writeContent("User:" + ruWikipediaBot.getLogin() + "/" + template + "/P"
					+ entry.getKey().getId(), null, entry.getValue().toString(), null, "Update reconsiliaction report",
					true, false);
		}
	}

}
