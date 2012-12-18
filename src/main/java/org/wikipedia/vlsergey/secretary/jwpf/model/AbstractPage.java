package org.wikipedia.vlsergey.secretary.jwpf.model;

public abstract class AbstractPage implements Page {
	@Override
	public int compareTo(Page o) {
		if (this.getId() == null || o.getId() == null)
			return 0;

		return this.getId().compareTo(o.getId());
	}
}
