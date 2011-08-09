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
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;

import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;

@Entity(name = "Revision")
public class StoredRevision implements Revision {

	private Boolean anon;

	private Boolean bot;

	private String comment = null;

	private String content = null;

	private Long id = null;

	private Boolean minor;

	private StoredPage page;

	private Long size = null;

	private Date timestamp = null;

	private String user = null;

	public Boolean getAnon() {
		return anon;
	}

	public Boolean getBot() {
		return bot;
	}

	@Column(length = 1 << 10)
	public String getComment() {
		return comment;
	}

	@Lob
	@Column(length = 100 * 1 << 20)
	public String getContent() {
		return content;
	}

	@Id
	public Long getId() {
		return id;
	}

	public Boolean getMinor() {
		return minor;
	}

	@ManyToOne
	public StoredPage getPage() {
		return page;
	}

	public Long getSize() {
		return size;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	@Column(name = "username")
	public String getUser() {
		return user;
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

	public void setId(Long id) {
		this.id = id;
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

	@Override
	public String toString() {
		return "Revision [" + getPage() + "; " + getId() + "]";
	}

}
