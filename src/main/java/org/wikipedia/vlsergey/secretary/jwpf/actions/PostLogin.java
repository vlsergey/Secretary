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

import java.text.ParseException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.w3c.dom.Element;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ActionException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.LoginData;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.TicketData;

public class PostLogin extends AbstractApiXmlAction {

	private static final Log log = LogFactory.getLog(PostLogin.class);

	private final static String needToken = "NeedToken";

	private final static String notExists = "NotExists";

	private final static String success = "Success";

	private final static String wrongPass = "WrongPass";

	private String exceptionText = "";

	private LoginData login = null;

	private final String password;

	private TicketData ticketData = null;

	private final String username;

	/**
	 * 
	 * @param username
	 *            the
	 * @param pw
	 *            password
	 */
	public PostLogin(boolean isBot, final String username, final String password) {
		super(isBot);
		this.username = username;
		this.password = password;

		log.info("[action=login]: " + username);

		HttpPost pm = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setFormatXml(multipartEntity);
		setMaxLag(multipartEntity);

		setParameter(multipartEntity, "action", "login");
		setParameter(multipartEntity, "lgname", username);
		setParameter(multipartEntity, "lgpassword", password);

		pm.setEntity(multipartEntity);
		msgs.add(pm);
	}

	private void findContent(final Element api) {
	}

	public PostPostLogin getConfirmationAction() {
		return new PostPostLogin(isBot(), ticketData, username, password);
	}

	public LoginData getLoginData() throws ActionException {
		if (login == null) {
			throw new ActionException(exceptionText);
		}
		return login;
	}

	public boolean needConfirmation() {
		return ticketData != null;
	}

	@Override
	protected void parseAPI(org.w3c.dom.Element root) throws ProcessException, ParseException {
		Element login = (Element) root.getElementsByTagName("login").item(0);
		String result = login.getAttribute("result");
		if (result.equalsIgnoreCase(success)) {
			try {
				this.login = new LoginData(Integer.parseInt(login.getAttribute("lguserid")),
						login.getAttribute("lgusername"), login.getAttribute("lgtoken"));
			} catch (NumberFormatException e) {
				e.printStackTrace();
			}
		} else if (result.equalsIgnoreCase(needToken)) {
			this.ticketData = new TicketData(login.getAttribute("token"), login.getAttribute("sessionid"));
		} else if (result.equalsIgnoreCase(wrongPass)) {
			exceptionText = "Wrong Password";
		} else if (result.equalsIgnoreCase(notExists)) {
			exceptionText = "No sutch User";
		}
	}

}
