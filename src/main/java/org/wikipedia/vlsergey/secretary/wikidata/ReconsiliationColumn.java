package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.dom.AbstractContainer;
import org.wikipedia.vlsergey.secretary.dom.ArticleFragment;
import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.dom.TemplatePart;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Entity;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Reference;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;

public abstract class ReconsiliationColumn {

	protected static final EntityId ITEM_RUWIKI = EntityId.item(206855);

	protected static Reference REFERENCE_FROM_RUWIKI = new Reference();

	static {
		REFERENCE_FROM_RUWIKI.addSnak(Snak.newSnak(Properties.IMPORTED_FROM, ITEM_RUWIKI));
	}

	protected static void fillToWikidata(final EntityId property, Collection<ValueWithQualifiers> source,
			JSONObject result, List<String> claimIdsToBeDeleted) {
		for (ValueWithQualifiers newValue : source) {
			Statement statement = Statement.newStatement(property, newValue.getValue());
			// add qualifiers
			for (Snak qualifier : newValue.getQualifiers()) {
				statement.addQualifier(qualifier);
			}
			statement.addReference(REFERENCE_FROM_RUWIKI);
			if (claimIdsToBeDeleted != null && !claimIdsToBeDeleted.isEmpty()) {
				statement.setId(claimIdsToBeDeleted.remove(0));
			}
			Entity.putProperty(result, statement);
		}
	}

	protected static List<ValueWithQualifiers> fromWikidata(Entity entity, final EntityId property,
			boolean ignoreRuwikiImport) {
		List<ValueWithQualifiers> result = new LinkedList<>();
		for (Statement statement : entity.getClaims(property)) {
			if (ignoreRuwikiImport && statement.isImportedFrom(ITEM_RUWIKI)) {
				continue;
			}
			result.add(new ValueWithQualifiers(statement));
		}
		return result;
	}

	static Content prepareParameterValue(Content content) {
		if (content instanceof AbstractContainer) {
			AbstractContainer container = (AbstractContainer) content;
			final List<? extends Content> originalChildren = container.getChildren();
			List<Content> children = new ArrayList<>(originalChildren.size());
			for (Content child : originalChildren) {
				if (child instanceof Template) {
					Template template = (Template) child;
					if (StringUtils.equalsIgnoreCase("s", template.getName().toWiki(true))
							&& template.getParameters().size() == 1) {
						children.add(template.getParameter(0));
					} else {
						children.add(template);
					}
				} else {
					children.add(prepareParameterValue(child));
				}
			}
			return new ArticleFragment(children);
		}
		return content;
	}

	protected final DataType dataType;

	protected final Function<String, List<ValueWithQualifiers>> parseF;

	protected final EntityId[] properties;

	protected final Collection<String> templateParameters;

	public ReconsiliationColumn(List<String> templateParameters, DataType dataType, EntityId[] properties,
			Function<String, List<ValueWithQualifiers>> parseF) {
		this.templateParameters = templateParameters;
		this.dataType = dataType;
		this.properties = properties;
		this.parseF = parseF;
	}

	public abstract void fillToWikidata(Collection<ValueWithQualifiers> source, JSONObject result,
			Map<EntityId, List<String>> claimIdsToBeDeletedMap);

	public abstract List<ValueWithQualifiers> fromWikidata(Entity entity, boolean ignoreRuwikiImport);

	public List<ValueWithQualifiers> fromWikipedia(Template template,
			Consumer<UnsupportedParameterValueException> exceptionCounter) {
		List<ValueWithQualifiers> result = new LinkedList<>();
		for (String templateParameter : templateParameters) {
			for (TemplatePart part : template.getParameters(templateParameter)) {
				final Content parameterValue = part.getValue();
				if (parameterValue != null) {
					try {
						String value = StringUtils.trimToEmpty(prepareParameterValue(parameterValue).toWiki(true));
						if (StringUtils.isNotBlank(value)) {
							result.addAll(parseF.apply(value));
						}
					} catch (UnsupportedParameterValueException exc) {
						exc.setTemplatePartValue(parameterValue);
						exceptionCounter.accept(exc);
						throw exc;
					}
				}
			}
		}
		return result;
	}

	public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipedia,
			Collection<ValueWithQualifiers> wikidata) {
		if (wikipedia.isEmpty()) {
			return ReconsiliationAction.remove_from_wikipedia_as_empty;
		}
		if (wikidata.isEmpty()) {
			return ReconsiliationAction.append;
		}
		if (wikidata.containsAll(wikipedia)) {
			return ReconsiliationAction.remove_from_wikipedia_as_not_empty;
		}
		return ReconsiliationAction.report_difference;
	}

	public abstract String getCode();

}