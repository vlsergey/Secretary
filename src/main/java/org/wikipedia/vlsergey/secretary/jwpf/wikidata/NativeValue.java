package org.wikipedia.vlsergey.secretary.jwpf.wikidata;

public abstract class NativeValue implements Value {

	@Override
	public void setType(ValueType type) {
		throw new UnsupportedOperationException("Can't change type of value");
	}

}
