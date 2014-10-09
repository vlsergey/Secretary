package org.wikipedia.vlsergey.secretary.wikidata;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.PropertyGroups;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class ChangeQualifierType implements Runnable {

	@Autowired
	private WikidataBot wikidataBot;

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	private void convert(Statement statement, Snak titleQualifier) {
		Snak partQualifier = new Snak();
		partQualifier.setDataType(DataType.STRING);
		partQualifier.setDatavalue(titleQualifier.getStringValue());
		partQualifier.setProperty(Properties.SECTION_VERSE_OR_PARAGRAPH);
		partQualifier.setSnakType(titleQualifier.getSnakType());
		statement.addQualifier(partQualifier);
		statement.removeQualifier(titleQualifier);
	}

	@Override
	public void run() {

		final Long propertyPageId = wikidataCache.queryLatestRevision(Properties.DESCRIBED_BY.getPageTitle()).getPage()
				.getId();

		for (Revision revision : wikidataCache.queryByBacklinks(propertyPageId, Namespace.NSS_MAIN)) {
			final Entity entity = new Entity(new JSONObject(revision.getContent()));
			boolean updateEntity = false;
			JSONObject newData = new JSONObject();

			for (Statement statement : entity.getClaims(Properties.DESCRIBED_BY)) {
				boolean updateStatement = false;
				for (EntityId source : Sources.SOURCES_USED_IN_DESCRIBED_BY) {
					if (statement.isWikibaseEntityIdValue(source)) {
						if (!statement.getQualifiers(Properties.TITLE).isEmpty()
								&& statement.getQualifiers(Properties.SECTION_VERSE_OR_PARAGRAPH).isEmpty()) {
							for (Snak titleQualifier : statement.getQualifiers(Properties.TITLE)) {
								convert(statement, titleQualifier);
								updateStatement = true;
							}
						}
					}
				}
				if (updateStatement) {
					Entity.putProperty(newData, statement);
					updateEntity = true;
				}
			}

			for (EntityId sourceProperty : PropertyGroups.SOURCES) {
				for (Statement statement : entity.getClaims(sourceProperty)) {
					boolean updateStatement = false;
					if (!statement.getQualifiers(Properties.TITLE).isEmpty()
							&& statement.getQualifiers(Properties.SECTION_VERSE_OR_PARAGRAPH).isEmpty()) {
						for (Snak titleQualifier : statement.getQualifiers(Properties.TITLE)) {
							convert(statement, titleQualifier);
							updateStatement = true;
						}
					}
					if (updateStatement) {
						Entity.putProperty(newData, statement);
						updateEntity = true;
					}
				}
			}

			if (updateEntity) {
				wikidataBot.wgEditEntity(entity, newData, "Change [[" + Properties.TITLE.getPageTitle() + "]] → [["
						+ Properties.SECTION_VERSE_OR_PARAGRAPH.getPageTitle() + "]]");
			}
		}

	}
}
