/*
 * Copyright 2007 Thomas Stock.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 * 
 * Contributors:
 * 
 */
package org.wikipedia.vlsergey.secretary.cache;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

@Entity(name = "Revision")
public class StoredRevision implements Revision {

	private Boolean anon;

	private Boolean bot;

	private String comment = null;

	private byte[] content = null;

	private StoredRevisionPk key;

	private Boolean minor;

	private StoredPage page;

	private Long size = null;

	private Date timestamp = null;

	private String user = null;

	private byte[] xml = null;

	@Override
	public Boolean getAnon() {
		return anon;
	}

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

	@Override
	public Boolean getBot() {
		return bot;
	}

	@Override
	@Column(length = 1 << 10)
	public String getComment() {
		return comment;
	}

	@Override
	@Transient
	public String getContent() {
		return IoUtils.stringFromBinary(getBinaryContent(), true);
	}

	@Override
	@Transient
	public Long getId() {
		return getKey().getRevisionId();
	}

	@EmbeddedId
	public StoredRevisionPk getKey() {
		return key;
	}

	@Override
	public Boolean getMinor() {
		return minor;
	}

	@Override
	@ManyToOne
	public StoredPage getPage() {
		return page;
	}

	@Override
	public Long getSize() {
		return size;
	}

	@Override
	public Date getTimestamp() {
		return timestamp;
	}

	@Override
	@Column(name = "username")
	public String getUser() {
		return user;
	}

	@Override
	@Transient
	public String getXml() {
		return IoUtils.stringFromBinary(getBinaryXml(), false);
	}

	@Override
	public boolean hasContent() {
		return getBinaryContent() != null && getBinaryContent().length > 0
				&& (getBinaryContent().length > 150 || StringUtils.isNotEmpty(getContent()));
	}

	@Override
	public boolean hasXml() {
		return getBinaryXml() != null && getBinaryXml().length > 0 && StringUtils.isNotEmpty(getXml());
	}

	public void setAnon(Boolean anon) {
		this.anon = anon;
	}

	protected void setBinaryContent(byte[] content) {
		this.content = content;
	}

	protected void setBinaryXml(byte[] xml) {
		this.xml = xml;
	}

	public void setBot(Boolean bot) {
		this.bot = bot;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Transient
	public void setContent(String content) {
		setBinaryContent(IoUtils.stringToBinary(content, true));
	}

	public void setKey(StoredRevisionPk key) {
		this.key = key;
	}

	public void setMinor(Boolean minor) {
		this.minor = minor;
	}

	public void setPage(StoredPage page) {
		this.page = page;
	}

	public void setSize(Long size) {
		this.size = size;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	public void setUser(String user) {
		this.user = user;
	}

	@Transient
	public void setXml(String xml) {
		setBinaryXml(IoUtils.stringToBinary(xml, true));
	}

	@Override
	public String toString() {
		return "Revision [" + getPage() + "; " + getId() + "]";
	}

}
