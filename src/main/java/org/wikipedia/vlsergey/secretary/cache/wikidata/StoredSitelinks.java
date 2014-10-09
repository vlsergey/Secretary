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
package org.wikipedia.vlsergey.secretary.cache.wikidata;

import java.util.Map;

import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.MapKeyColumn;

@Entity
public class StoredSitelinks {

	private Long pageId;

	private Long revision;

	private Map<String, String> sitelinks;

	public Long getPageId() {
		return pageId;
	}

	@Id
	public Long getRevision() {
		return revision;
	}

	@ElementCollection(fetch = FetchType.LAZY)
	@MapKeyColumn(name = "site")
	@Column(name = "title")
	public Map<String, String> getSitelinks() {
		return sitelinks;
	}

	public void setPageId(Long pageId) {
		this.pageId = pageId;
	}

	public void setRevision(Long revision) {
		this.revision = revision;
	}

	public void setSitelinks(Map<String, String> sitelinks) {
		this.sitelinks = sitelinks;
	}
}
