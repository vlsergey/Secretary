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

import java.util.Set;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import org.wikipedia.vlsergey.secretary.jwpf.model.AbstractPage;

@Entity(name = "Page")
public class StoredPage extends AbstractPage {

	private StoredPagePk key;

	private Set<StoredPage> links;

	private Boolean missing = false;

	private Integer namespace;

	private String title;

	@Override
	@Transient
	public Long getId() {
		return getKey().getPageId();
	}

	@EmbeddedId
	public StoredPagePk getKey() {
		return key;
	}

	@Override
	@ManyToMany
	public Set<StoredPage> getLinks() {
		return links;
	}

	@Override
	public Boolean getMissing() {
		return missing;
	}

	@Override
	public Integer getNamespace() {
		return namespace;
	}

	@Override
	public String getTitle() {
		return title;
	}

	public void setKey(StoredPagePk key) {
		this.key = key;
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

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String toString() {
		return "#" + getId() + ": " + getTitle();
	}

}
