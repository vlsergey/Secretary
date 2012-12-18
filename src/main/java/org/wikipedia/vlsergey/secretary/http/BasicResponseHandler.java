package org.wikipedia.vlsergey.secretary.http;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

public class BasicResponseHandler implements ResponseHandler<String> {

	private final String defaultEncoding;

	public BasicResponseHandler(final String defaultEncoding) {
		this.defaultEncoding = defaultEncoding;
	}

	@Override
	public String handleResponse(final HttpResponse response) throws HttpResponseException, IOException {
		StatusLine statusLine = response.getStatusLine();
		if (statusLine.getStatusCode() >= 300) {
			throw new HttpResponseException(statusLine.getStatusCode(), statusLine.getReasonPhrase());
		}

		HttpEntity entity = response.getEntity();
		return entity == null ? null : EntityUtils.toString(entity, defaultEncoding);
	}
}