package org.wikipedia.vlsergey.secretary.trust;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;

@Entity
public class RevisionAuthorship {

	private byte[] data;

	private RevisionAuthorshipPk key;

	@Lob
	public byte[] getData() {
		return data;
	}

	@EmbeddedId
	public RevisionAuthorshipPk getKey() {
		return key;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public void setKey(RevisionAuthorshipPk key) {
		this.key = key;
	}
}
