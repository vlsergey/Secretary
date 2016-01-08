package org.wikipedia.vlsergey.secretary.trust;

import java.io.Serializable;

import org.wikipedia.vlsergey.secretary.jwpf.model.UserKey;

public class TextChunk implements Comparable<TextChunk>, Serializable {

	private static final long serialVersionUID = 1L;

	private final int hashCode;

	public final String text;

	public final UserKey userKey;

	public TextChunk(final UserKey userKey, String text) {
		if (userKey == null)
			throw new IllegalArgumentException();

		this.userKey = userKey;
		this.text = text;
		this.hashCode = text.hashCode();
	}

	@Override
	public int compareTo(TextChunk o) {
		return text.compareTo(o.text);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;

		final TextChunk obj2 = (TextChunk) obj;
		return this.hashCode == obj2.hashCode && this.text.equals(obj2.text);
	}

	@Override
	public int hashCode() {
		return hashCode;
	}

	@Override
	public String toString() {
		return "{" + userKey + "}" + text;
	}
}