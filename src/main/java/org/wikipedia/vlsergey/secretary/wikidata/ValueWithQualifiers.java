package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Statement;

public class ValueWithQualifiers {

	public static List<ValueWithQualifiers> fromSnak(Snak Snak) {
		return Collections.singletonList(new ValueWithQualifiers(Snak, Collections.emptyList()));
	}

	public static List<ValueWithQualifiers> fromStatements(List<Statement> statements) {
		if (statements == null || statements.isEmpty()) {
			return Collections.emptyList();
		}
		return statements.stream().map(x -> new ValueWithQualifiers(x)).collect(Collectors.toList());
	}

	private final List<Snak> qualifiers;

	private final Snak value;

	public ValueWithQualifiers(Snak value, List<Snak> qualifiers) {
		super();
		this.value = value;
		this.qualifiers = qualifiers;
	}

	public ValueWithQualifiers(Statement statement) {
		this.value = statement.getMainSnak();
		this.qualifiers = Arrays.asList(statement.getQualifiers());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ValueWithQualifiers other = (ValueWithQualifiers) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		if (qualifiers == null) {
			if (other.qualifiers != null)
				return false;
		} else if (!qualifiers.equals(other.qualifiers))
			return false;
		return true;
	}

	public List<Snak> getQualifiers() {
		return qualifiers;
	}

	public Snak getValue() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		result = prime * result + ((qualifiers == null) ? 0 : qualifiers.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return toString(0, Locale.getDefault(), x -> x.toString());
	}

	public String toString(int level, Locale locale, Function<EntityId, String> labelResolver) {
		StringBuilder builder = new StringBuilder();
		builder.append(toString(locale, labelResolver, this.getValue()));
		for (Snak qualifier : this.getQualifiers()) {
			builder.append("\n");
			builder.append(StringUtils.repeat("*", level + 1));
			builder.append(" [[:d:");
			builder.append(qualifier.getProperty().getPageTitle());
			builder.append("|");
			builder.append(labelResolver.apply(qualifier.getProperty()));
			builder.append("]] → ");
			try {
				builder.append(toString(locale, labelResolver, qualifier));
			} catch (Exception exc) {
				builder.append("(error)");
			}
		}
		return builder.toString();
	}

	private String toString(Locale locale, Function<EntityId, String> labelResolver, final Snak snak) {
		if (snak.getSnakType() == SnakType.value) {
			return snak.getDataValue().toWiki(locale, labelResolver).toWiki(true);
		} else {
			return "(" + snak.getSnakType() + ")";
		}
	}

}
