package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang.StringUtils;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiSnak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiStatement;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Snak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.SnakType;

public class ValueWithQualifiers {

	public static List<ValueWithQualifiers> fromSnak(ApiSnak apiSnak) {
		return Collections.singletonList(new ValueWithQualifiers(apiSnak, Collections.emptyList()));
	}

	private final List<ApiSnak> qualifiers;

	private final ApiSnak value;

	public ValueWithQualifiers(ApiSnak value, List<ApiSnak> qualifiers) {
		super();
		this.value = value;
		this.qualifiers = qualifiers;
	}

	public ValueWithQualifiers(ApiStatement statement) {
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

	public List<ApiSnak> getQualifiers() {
		return qualifiers;
	}

	public ApiSnak getValue() {
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

	public String toString(Function<EntityId, String> labelResolver, int level) {
		StringBuilder builder = new StringBuilder();
		builder.append(toString(labelResolver, this.getValue()));
		for (Snak qualifier : this.getQualifiers()) {
			builder.append("\n");
			builder.append(StringUtils.repeat("*", level + 1));
			builder.append(" [[:d:Property:");
			builder.append(qualifier.getProperty());
			builder.append("|");
			builder.append(labelResolver.apply(qualifier.getProperty()));
			builder.append("]] → ");
			builder.append(toString(labelResolver, qualifier));
		}
		return builder.toString();
	}

	private String toString(Function<EntityId, String> labelResolver, final Snak snak) {
		if (snak.getSnakType() == SnakType.value) {
			return snak.getDataValue().toWiki(labelResolver).toWiki(true);
		} else {
			return "(" + snak.getSnakType() + ")";
		}
	}

}
