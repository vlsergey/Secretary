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
 * Philipp Kohl 
 */
package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntity;
import org.jdom.DataConversionException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.LoginData;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.TicketData;
import org.xml.sax.InputSource;

/**
 * 
 * @author Thomas Stock
 * @supportedBy MediaWiki 1.9.x
 */
public class PostPostLogin extends MWAction {

	private static final Log log = LogFactory.getLog(PostPostLogin.class);

	private final static String notExists = "NotExists";

	private final static String success = "Success";

	private final static String wrongPass = "WrongPass";

	private String exceptionText = "";

	private LoginData login = null;

	/**
	 * 
	 * @param username
	 *            the
	 * @param pw
	 *            password
	 */
	protected PostPostLogin(final TicketData ticketData, String username,
			final String password) {
		HttpPost pm = new HttpPost("/api.php?action=login&format=xml");

		MultipartEntity multipartEntity = new MultipartEntity();
		setParameter(multipartEntity, "lgname", username);
		setParameter(multipartEntity, "lgpassword", password);
		setParameter(multipartEntity, "lgtoken", ticketData.getToken());

		pm.setEntity(multipartEntity);
		msgs.add(pm);
	}

	private void findContent(final Element api) {
		Element login = api.getChild("login");
		String result = login.getAttributeValue("result");
		if (result.equalsIgnoreCase(success)) {
			try {
				this.login = new LoginData(login.getAttribute("lguserid")
						.getIntValue(), login.getAttributeValue("lgusername"),
						login.getAttributeValue("lgtoken"));
			} catch (DataConversionException e) {
				e.printStackTrace();
			}
		} else if (result.equalsIgnoreCase(wrongPass)) {
			exceptionText = "Wrong Password";
		} else if (result.equalsIgnoreCase(notExists)) {
			exceptionText = "No sutch User";
		}
	}

	public LoginData getLoginData() throws ActionException {
		if (login == null) {
			throw new ActionException(exceptionText);
		}
		return login;
	}

	/**
	 * @param s
	 *            incomming
	 * @return after testing
	 */
	public void processReturningText(final HttpRequestBase hm, final String s)
			throws ProcessException {
		SAXBuilder builder = new SAXBuilder();
		Element root = null;
		try {
			Reader i = new StringReader(s);
			Document doc = builder.build(new InputSource(i));

			root = doc.getRootElement();

		} catch (JDOMException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.debug(s);
		findContent(root);
	}
}
