package org.wikipedia.vlsergey.secretary.cache;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import org.wikipedia.vlsergey.secretary.utils.IoUtils;

@Entity(name = "XmlCache")
public class XmlCacheItem {

	private byte[] content = null;

	private String hash;

	private byte[] xml = null;

	@Lob
	@Column(length = 100 * 1 << 20)
	protected byte[] getBinaryContent() {
		return content;
	}

	@Lob
	@Column(length = 100 * 1 << 20)
	protected byte[] getBinaryXml() {
		return xml;
	}

	@Transient
	public String getContent() {
		return IoUtils.stringFromBinary(getBinaryContent(), true);
	}

	@Id
	public String getHash() {
		return hash;
	}

	@Transient
	public String getXml() {
		return IoUtils.stringFromBinary(getBinaryXml(), true);
	}

	protected void setBinaryContent(byte[] content) {
		this.content = content;
	}

	protected void setBinaryXml(byte[] xml) {
		this.xml = xml;
	}

	@Transient
	public void setContent(String content) {
		setBinaryContent(IoUtils.stringToBinary(content, true));
	}

	public void setHash(String hash) {
		this.hash = hash;
	}

	@Transient
	public void setXml(String xml) {
		setBinaryXml(IoUtils.stringToBinary(xml, true));
	}

	@Override
	public String toString() {
		return "XmlCacheItem [" + getHash() + "]";
	}
}
