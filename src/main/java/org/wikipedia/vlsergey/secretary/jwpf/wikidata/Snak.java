package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public interface Snak {

	String getDatatype();

	String getHash();

	EntityId getProperty();

	SnakType getSnakType();

	StringValue getStringValue();

	WikibaseEntityIdValue getWikibaseEntityIdValue();

	boolean hasSnakType();

	void setDatatype(String value);

	void setDatavalue(DataValue value);

	void setProperty(EntityId value);

	void setSnakType(SnakType value);

}