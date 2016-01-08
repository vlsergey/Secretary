package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.mutable.MutableLong;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.dom.AbstractContainer;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;

@Component
public class ConstrainCheckerQualifiers implements Runnable {

	@Autowired
	@Qualifier("wikidataCache")
	private WikiCache wikidataCache;

	@Override
	public void run() {

		for (Revision propertyTalkRevision : wikidataCache.queryByEmbeddedIn("Template:Constraint:Qualifiers",
				Namespace.NSS_PROPERTY_TALK)) {
			try {
				EntityId propertyToCheck = EntityId.parse(propertyTalkRevision.getPage().getTitle()
						.substring("Property talk:".length()));
				ArticleFragment fragment = wikidataCache.getMediaWikiBot().getXmlParser().parse(propertyTalkRevision);
				List<EntityId> allowedQualifiers = new ArrayList<>();

				for (Template template : fragment.getTemplates("Constraint:Qualifiers")) {
					for (TemplatePart templatePart : template.getParameters("list")) {
						AbstractContainer container = (AbstractContainer) templatePart.getValue();
						for (Template pTemplate : container.getTemplates("P")) {
							String property = pTemplate.getParameter(0).getValue().toWiki(true);
							if (property.startsWith("P")) {
								property = property.substring(1);
							}
							allowedQualifiers.add(EntityId.property(Long.parseLong(property)));
						}
					}
				}

				System.err.println("Need to check that claims of " + propertyToCheck + " has only qualifiers: "
						+ allowedQualifiers);

				SortedMap<EntityId, MutableLong> countByQualifier = new TreeMap<>();
				SortedMap<EntityId, Set<EntityId>> failures = new TreeMap<>();

				final Long propertyPageId = wikidataCache.queryLatestRevision("Property:" + propertyToCheck).getPage()
						.getId();
				for (Revision toCheck : wikidataCache.queryByBacklinks(propertyPageId, Namespace.NSS_MAIN)) {
					final Entity entity = new Entity(new JSONObject(toCheck.getContent()));
					final EntityId entityId = entity.getId();

					for (Statement statement : entity.getClaims(propertyToCheck)) {
						for (Snak snak : statement.getQualifiers()) {
							final EntityId qualifierPropertyId = snak.getProperty();
							if (allowedQualifiers.contains(qualifierPropertyId)) {
								continue;
							}
							if (!failures.containsKey(entityId)) {
								failures.put(entityId, new TreeSet<EntityId>());
							}
							failures.get(entityId).add(qualifierPropertyId);

							if (!countByQualifier.containsKey(qualifierPropertyId)) {
								countByQualifier.put(qualifierPropertyId, new MutableLong(0));
							}
							countByQualifier.get(qualifierPropertyId).increment();
						}
					}
				}

				StringBuilder stringBuilder = new StringBuilder();
				stringBuilder.append("{{/Header}}\n\n");

				for (EntityId key : countByQualifier.keySet()) {
					stringBuilder.append("* {{P|");
					stringBuilder.append(key);
					stringBuilder.append("}} — ");
					stringBuilder.append(countByQualifier.get(key).longValue());
					stringBuilder.append("\n");
				}

				stringBuilder.append("\n\n");
				for (EntityId key : failures.keySet()) {
					stringBuilder.append("* [[");
					stringBuilder.append(key);
					stringBuilder.append("]] —");
					for (EntityId wrongQualifier : failures.get(key)) {
						stringBuilder.append(" [[Property:");
						stringBuilder.append(wrongQualifier);
						stringBuilder.append("|");
						stringBuilder.append(wrongQualifier);
						stringBuilder.append("]];");
					}
					stringBuilder.append("\n");
				}

				wikidataCache.getMediaWikiBot().writeContent(
						"Wikidata:Database reports/Constraint violations/" + propertyToCheck + "/Qualifiers", null,
						stringBuilder.toString(), null, "Update constrains report", true, false);

				System.err.println("Failures: " + failures);

			} catch (Exception exc) {
				exc.printStackTrace();
			}
		}
	}
}
