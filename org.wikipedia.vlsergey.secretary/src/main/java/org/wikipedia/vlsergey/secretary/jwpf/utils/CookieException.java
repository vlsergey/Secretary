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
 * 
 * @author Thomas Stock
 * 
 */
public class CookieException extends ActionException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * 
	 *
	 */
	public CookieException() {
		// do nothing
	}

	/**
	 * 
	 * @param arg0
	 *            exception text
	 */
	public CookieException(String arg0) {
		super(arg0);
	}

	/**
	 * 
	 * @param arg0
	 *            exception text
	 * @param arg1
	 *            sub
	 */
	public CookieException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	/**
	 * 
	 * @param arg0
	 *            sub
	 */
	public CookieException(Throwable arg0) {
		super(arg0);
	}

}
