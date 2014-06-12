package org.wikipedia.vlsergey.secretary.jwpf;

import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;

public class DatabaseLagException extends ClientProtocolException {

	private static final long serialVersionUID = 1L;

	public final Header databaseLag;

	public final Header retryAfter;

	public DatabaseLagException(Header databaseLag, Header retryAfter) {
		this.databaseLag = databaseLag;
		this.retryAfter = retryAfter;
	}

}
