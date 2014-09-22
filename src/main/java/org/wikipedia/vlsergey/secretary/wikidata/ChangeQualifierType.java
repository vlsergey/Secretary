package org.wikipedia.vlsergey.secretary.wikidata;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiEntity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiSnak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiStatement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.PropertyGroups;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.WikidataBot;

@Component
public class ChangeQualifierType implements Runnable {

	@Autowired
	private WikidataBot wikidataBot;

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	@Override
	public void run() {

		final Long propertyPageId = wikidataCache.queryLatestRevision(Properties.DESCRIBED_BY.getPageTitle()).getPage()
				.getId();

		for (Revision revision : wikidataCache.queryByBacklinks(propertyPageId, Namespace.NSS_MAIN)) {
			final ApiEntity entity = new ApiEntity(new JSONObject(revision.getContent()));
			boolean updateEntity = false;
			JSONObject newData = new JSONObject();

			for (ApiStatement statement : entity.getClaims(Properties.DESCRIBED_BY)) {
				boolean updateStatement = false;
				for (EntityId source : Sources.SOURCES_USED_IN_DESCRIBED_BY) {
					if (statement.isWikibaseEntityIdValue(source)) {
						if (statement.getQualifiers(Properties.TITLE).length != 0
								&& statement.getQualifiers(Properties.SECTION_VERSE_OR_PARAGRAPH).length == 0) {
							for (ApiSnak titleQualifier : statement.getQualifiers(Properties.TITLE)) {
								convert(statement, titleQualifier);
								updateStatement = true;
							}
						}
					}
				}
				if (updateStatement) {
					ApiEntity.putProperty(newData, statement);
					updateEntity = true;
				}
			}

			for (EntityId sourceProperty : PropertyGroups.SOURCES) {
				for (ApiStatement statement : entity.getClaims(sourceProperty)) {
					boolean updateStatement = false;
					if (statement.getQualifiers(Properties.TITLE).length != 0
							&& statement.getQualifiers(Properties.SECTION_VERSE_OR_PARAGRAPH).length == 0) {
						for (ApiSnak titleQualifier : statement.getQualifiers(Properties.TITLE)) {
							convert(statement, titleQualifier);
							updateStatement = true;
						}
					}
					if (updateStatement) {
						ApiEntity.putProperty(newData, statement);
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

	private void convert(ApiStatement statement, ApiSnak titleQualifier) {
		ApiSnak partQualifier = new ApiSnak();
		partQualifier.setDataType(DataType.STRING);
		partQualifier.setDatavalue(titleQualifier.getStringValue());
		partQualifier.setProperty(Properties.SECTION_VERSE_OR_PARAGRAPH);
		partQualifier.setSnakType(titleQualifier.getSnakType());
		statement.addQualifier(partQualifier);
		statement.removeQualifier(titleQualifier);
	}
}
