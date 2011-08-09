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
package org.wikipedia.vlsergey.secretary.cache;

import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;

import org.wikipedia.vlsergey.secretary.jwpf.model.Page;

@Entity(name = "Page")
public class StoredPage implements Page {

	private Set<StoredPage> categories;

	// List<ImageInfo> imageInfo;

	private Long id;

	private Set<StoredPage> links;

	private Boolean missing = false;

	private Integer namespace;

	private List<StoredRevision> revisions;

	private String title;

	@ManyToMany
	public Set<StoredPage> getCategories() {
		return categories;
	}

	// public List<ImageInfo> getImageInfo() {
	// return imageInfo;
	// }

	@Id
	public Long getId() {
		return id;
	}

	@ManyToMany
	public Set<StoredPage> getLinks() {
		return links;
	}

	public Boolean getMissing() {
		return missing;
	}

	public Integer getNamespace() {
		return namespace;
	}

	@OneToMany
	public List<StoredRevision> getRevisions() {
		return revisions;
	}

	public String getTitle() {
		return title;
	}

	public void setCategories(Set<StoredPage> categories) {
		this.categories = categories;
	}

	// public void setImageInfo(List<ImageInfo> imageInfo) {
	// this.imageInfo = imageInfo;
	// }

	public void setId(Long pageID) {
		this.id = pageID;
	}

	public void setLinks(Set<StoredPage> links) {
		this.links = links;
	}

	public void setMissing(Boolean missing) {
		this.missing = missing;
	}

	public void setNamespace(Integer namespace) {
		this.namespace = namespace;
	}

	public void setRevisions(List<StoredRevision> revisions) {
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
