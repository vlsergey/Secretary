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
package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.util.Date;
import java.util.List;

import javax.persistence.Transient;

import org.apache.commons.lang3.StringUtils;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

public class ParsedRevision extends AbstractRevision {

	private Boolean anon;

	private byte[] binaryContent = null;

	private byte[] binaryXml = null;

	private Boolean bot;

	private String comment = null;

	private List<RevisionFlagged> flagged = null;

	private Long id = null;

	private Boolean minor;

	private final Page page;

	private Long size = null;

	private Date timestamp = null;

	private String user = null;

	private Long userId = null;

	public ParsedRevision(Page page) {
		this.page = page;
	}

	@Override
	public Boolean getAnon() {
		return anon;
	}

	byte[] getBinaryContent() {
		return binaryContent;
	}

	byte[] getBinaryXml() {
		return binaryXml;
	}

	@Override
	public Boolean getBot() {
		return bot;
	}

	@Override
	public String getComment() {
		return comment;
	}

	@Override
	@Transient
	public String getContent() {
		return IoUtils.stringFromBinary(getBinaryContent(), true);
	}

	public List<RevisionFlagged> getFlagged() {
		return flagged;
	}

	@Override
	public Long getId() {
		return id;
	}

	@Override
	public Boolean getMinor() {
		return minor;
	}

	@Override
	public Page getPage() {
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
	public String getUser() {
		return user;
	}

	@Override
	public Long getUserId() {
		return userId;
	}

	@Override
	public String getXml() {
		return IoUtils.stringFromBinary(getBinaryXml(), true);
	}

	@Override
	public boolean hasContent() {
		return getBinaryContent() != null && getBinaryContent().length > 0 && StringUtils.isNotEmpty(getContent());
	}

	@Override
	public boolean hasXml() {
		return getBinaryXml() != null && getBinaryXml().length > 0 && StringUtils.isNotEmpty(getXml());
	}

	public void setAnon(Boolean anon) {
		this.anon = anon;
	}

	void setBinaryContent(byte[] binaryContent) {
		this.binaryContent = binaryContent;
	}

	void setBinaryXml(byte[] binaryXml) {
		this.binaryXml = binaryXml;
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

	public void setFlagged(List<RevisionFlagged> flagged) {
		this.flagged = flagged;
	}

	public void setId(Long revisionID) {
		this.id = revisionID;
	}

	public void setMinor(Boolean minor) {
		this.minor = minor;
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

	public void setUserId(Long userid) {
		this.userId = userid;
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
