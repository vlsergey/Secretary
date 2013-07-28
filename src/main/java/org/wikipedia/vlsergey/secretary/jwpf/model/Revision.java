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

/**
 * 
 * @author Thomas Stock
 */
public interface Revision {

	Boolean getAnon();

	Boolean getBot();

	String getComment();

	String getContent();

	Long getId();

	Boolean getMinor();

	Page getPage();

	Long getSize();

	Date getTimestamp();

	String getUser();

	String getXml();

	boolean hasContent();

	boolean hasXml();
}
