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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Date;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wikipedia.vlsergey.secretary.jwpf.model.Revision;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

@Entity(name = "Revision")
public class StoredRevision implements Revision {

	private static final Logger logger = LoggerFactory
			.getLogger(StoredRevision.class);

	private static final String fromBinary(byte[] content) {
		if (content == null || content.length == 0)
			return StringUtils.EMPTY;

		try {
			final ByteArrayInputStream in = new ByteArrayInputStream(content);
			ZipInputStream zipInputStream = new ZipInputStream(in);
			ZipEntry zipEntry = zipInputStream.getNextEntry();
			String string = IoUtils.readToString(zipInputStream, "utf-8");
			return string;
		} catch (IOException exc) {
			throw new RuntimeException(exc);
		}
	}

	private static final byte[] toBinary(String content) {
		if (StringUtils.isEmpty(content)) {
			return null;
		}
		try {
			byte[] original = content.getBytes("utf-8");
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			ZipOutputStream zipOutputStream = new ZipOutputStream(out);
			zipOutputStream.setLevel(Deflater.BEST_COMPRESSION);
			zipOutputStream.putNextEntry(new ZipEntry("content"));
			zipOutputStream.write(original);
			zipOutputStream.close();
			byte[] compressed = out.toByteArray();
			logger.trace("Compressed " + original.length + " => "
					+ compressed.length);
			return compressed;
		} catch (IOException exc) {
			throw new RuntimeException(exc);
		}
	}

	private Boolean anon;

	private Boolean bot;

	private String comment = null;

	private byte[] content = null;

	private Long id = null;

	private Boolean minor;

	private StoredPage page;

	private Long size = null;

	private Date timestamp = null;

	private String user = null;

	@Override
	public Boolean getAnon() {
		return anon;
	}

	@Lob
	@Column(length = 100 * 1 << 20)
	protected byte[] getBinaryContent() {
		return content;
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
		return fromBinary(getBinaryContent());
	}

	@Override
	@Id
	public Long getId() {
		return id;
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

	public void setAnon(Boolean anon) {
		this.anon = anon;
	}

	protected void setBinaryContent(byte[] content) {
		this.content = content;
	}

	public void setBot(Boolean bot) {
		this.bot = bot;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@Transient
	public void setContent(String content) {
		setBinaryContent(toBinary(content));
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
