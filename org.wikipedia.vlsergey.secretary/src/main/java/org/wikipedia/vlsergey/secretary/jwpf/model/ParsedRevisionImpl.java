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

public class ParsedRevisionImpl implements Revision {

	private Boolean anon;

	private Boolean bot;

	private String comment = null;

	private String content = null;

	private Long id = null;

	private Boolean minor;

	private final Page page;

	private List<RevisionFlagged> flagged = null;

	private Long size = null;

	private Date timestamp = null;

	private String user = null;

	private String xml = null;

	public ParsedRevisionImpl(Page page) {
		this.page = page;
	}

	@Override
	public Boolean getAnon() {
		return anon;
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
	public String getContent() {
		return content;
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
	public String getXml() {
		return xml;
	}

	public void setAnon(Boolean anon) {
		this.anon = anon;
	}

	public void setBot(Boolean bot) {
		this.bot = bot;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void setContent(String content) {
		this.content = content;
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

	public void setXml(String xml) {
		this.xml = xml;
	}

	@Override
	public String toString() {
		return "Revision [" + getPage() + "; " + getId() + "]";
	}

}
