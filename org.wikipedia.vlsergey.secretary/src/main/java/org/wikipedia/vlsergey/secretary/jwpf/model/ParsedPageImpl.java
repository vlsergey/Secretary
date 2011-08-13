/*
 * Copyright 2001-2008 Fizteh-Center Lab., MIPT, Russia
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Created on 30.03.2008
 */
package org.wikipedia.vlsergey.secretary.jwpf.model;

import java.util.List;
import java.util.Set;

import javax.persistence.Id;

public class ParsedPageImpl implements Page {

	private Set<Page> categories;

	// List<ImageInfo> imageInfo;

	private Long id;

	private Set<Page> links;

	private Boolean missing = null;

	private Integer namespace;

	private List<Revision> revisions;

	private String title;

	public Set<Page> getCategories() {
		return categories;
	}

	// public List<ImageInfo> getImageInfo() {
	// return imageInfo;
	// }

	@Id
	public Long getId() {
		return id;
	}

	public Set<Page> getLinks() {
		return links;
	}

	public Boolean getMissing() {
		return missing;
	}

	public Integer getNamespace() {
		return namespace;
	}

	public List<Revision> getRevisions() {
		return revisions;
	}

	public String getTitle() {
		return title;
	}

	public void setCategories(Set<Page> categories) {
		this.categories = categories;
	}

	// public void setImageInfo(List<ImageInfo> imageInfo) {
	// this.imageInfo = imageInfo;
	// }

	public void setId(Long pageID) {
		this.id = pageID;
	}

	public void setLinks(Set<Page> links) {
		this.links = links;
	}

	public void setMissing(Boolean missing) {
		this.missing = missing;
	}

	public void setNamespace(Integer namespace) {
		this.namespace = namespace;
	}

	public void setRevisions(List<Revision> revisions) {
		this.revisions = revisions;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "#" + getId() + ": " + getTitle();
	}

}
