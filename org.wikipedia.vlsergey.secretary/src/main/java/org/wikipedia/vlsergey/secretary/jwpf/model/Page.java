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

public interface Page {
	Set<? extends Page> getCategories();

	// List<ImageInfo> getImageInfo();

	List<? extends Revision> getRevisions();

	Set<? extends Page> getLinks();

	Integer getNamespace();

	Long getId();

	String getTitle();

	Boolean getMissing();
}
