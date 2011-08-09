package org.wikipedia.vlsergey.secretary.jwpf.utils;

import java.io.Serializable;

public class ApiException extends ProcessException {

	/**
	 * Serial Version UID
	 * 
	 * @see Serializable
	 */
	private static final long serialVersionUID = -7457160363735541211L;

	private String msg = "";

	public ApiException(String code, String value) {
		msg = "API ERROR CODE: " + code + " VALUE: " + value;
	}

	@Override
	public String getMessage() {
		return msg;
	}

}
