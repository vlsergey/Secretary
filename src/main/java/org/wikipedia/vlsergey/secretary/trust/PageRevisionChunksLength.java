package org.wikipedia.vlsergey.secretary.trust;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;

@Entity
public class PageRevisionChunksLength {

	private byte[] data;

	private PageRevisionChunksLengthPk key;

	@Lob
	public byte[] getData() {
		return data;
	}

	@EmbeddedId
	public PageRevisionChunksLengthPk getKey() {
		return key;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public void setKey(PageRevisionChunksLengthPk key) {
		this.key = key;
	}

}
