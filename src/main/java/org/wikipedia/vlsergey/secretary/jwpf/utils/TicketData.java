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
package org.wikipedia.vlsergey.secretary.jwpf.utils;

/**
 * <code>
 * <?xml version="1.0" encoding="utf-8"?>
 * <api>
 *   <login
 *     result="NeedToken"
 *     token="b5780b6e2f27e20b450921d9461010b4"
 *     sessionid="17ab96bd8ffbe8ca58a78657a918558e"
 *   />
 * </api>
 * </code>
 */
public class TicketData {

	private final String sessionId;
	private final String token;

	public TicketData(String token, String sessionId) {
		this.token = token;
		this.sessionId = sessionId;
	}

	public String getSessionId() {
		return sessionId;
	}

	public String getToken() {
		return token;
	}
}
