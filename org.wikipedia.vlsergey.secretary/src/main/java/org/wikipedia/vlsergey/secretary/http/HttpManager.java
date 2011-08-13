package org.wikipedia.vlsergey.secretary.http;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;

public class HttpManager {

	private final AbstractHttpClient client;

	public HttpManager() {
		SchemeRegistry registry = new SchemeRegistry();
		registry.register(new Scheme("http", 80, PlainSocketFactory
				.getSocketFactory()));
		registry.register(new Scheme("https", 443, SSLSocketFactory
				.getSocketFactory()));

		ThreadSafeClientConnManager threadSafeClientConnManager = new ThreadSafeClientConnManager(
				registry);
		threadSafeClientConnManager.setDefaultMaxPerRoute(2);
		threadSafeClientConnManager.setMaxTotal(10);

		client = new DefaultHttpClient(threadSafeClientConnManager);
		client.getParams().setParameter("http.useragent", "Secretary/JWBF");

		HttpClientParams.setRedirecting(client.getParams(), true);
	}

	public final HttpResponse execute(HttpUriRequest request)
			throws IOException, ClientProtocolException {
		return client.execute(request);
	}

	public <T> T execute(HttpUriRequest request,
			ResponseHandler<? extends T> responseHandler) throws IOException,
			ClientProtocolException {
		return client.execute(request, responseHandler);
	}

	public final CookieStore getCookieStore() {
		return client.getCookieStore();
	}

}
