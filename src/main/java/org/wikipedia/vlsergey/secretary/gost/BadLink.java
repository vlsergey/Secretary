package org.wikipedia.vlsergey.secretary.gost;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.wikipedia.vlsergey.secretary.utils.IoUtils;

@Entity
public class BadLink {

	private byte[] binaryContent;

	private String name;

	private String url;

	@Lob
	protected byte[] getBinaryContent() {
		return binaryContent;
	}

	@Transient
	public String getContent() {
		return IoUtils.stringFromBinary(getBinaryContent(), true);
	}

	@Column(length = 5000)
	public String getName() {
		return name;
	}

	@Id
	public String getUrl() {
		return url;
	}

	protected void setBinaryContent(byte[] binaryContent) {
		this.binaryContent = binaryContent;
	}

	@Transient
	public void setContent(String content) {
		setBinaryContent(IoUtils.stringToBinary(content, true));
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setUrl(String url) {
		this.url = url;
	}

}
