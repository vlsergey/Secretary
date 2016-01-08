package org.wikipedia.vlsergey.secretary.jwpf;

import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;

public class ServerErrorException extends ClientProtocolException {

	private static final long serialVersionUID = 1L;

	private final StatusLine statusLine;

	public ServerErrorException(StatusLine statusLine) {
		this.statusLine = statusLine;
	}

	public StatusLine getStatusLine() {
		return statusLine;
	}
}
