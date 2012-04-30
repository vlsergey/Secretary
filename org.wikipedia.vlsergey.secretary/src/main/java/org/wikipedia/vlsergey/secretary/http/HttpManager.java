package org.wikipedia.vlsergey.secretary.http;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.scheme.SchemeSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

@Transactional(propagation = Propagation.NEVER)
public class HttpManager {

	private static final class SocksClientConnManager implements SchemeSocketFactory {

		private final String proxyHost;
		private final int proxyPort;

		public SocksClientConnManager(String host, int port) {
			this.proxyHost = host;
			this.proxyPort = port;
		}

		@Override
		public Socket connectSocket(final Socket socket, final InetSocketAddress remoteAddress,
				final InetSocketAddress localAddress, final HttpParams params) throws IOException,
				UnknownHostException, ConnectTimeoutException {
			if (remoteAddress == null) {
				throw new IllegalArgumentException("Remote address may not be null");
			}
			if (params == null) {
				throw new IllegalArgumentException("HTTP parameters may not be null");
			}
			Socket sock;
			if (socket != null) {
				sock = socket;
			} else {
				sock = createSocket(params);
			}
			if (localAddress != null) {
				sock.setReuseAddress(HttpConnectionParams.getSoReuseaddr(params));
				sock.bind(localAddress);
			}
			int timeout = HttpConnectionParams.getConnectionTimeout(params);
			try {
				sock.connect(remoteAddress, timeout);
			} catch (SocketTimeoutException ex) {
				throw new ConnectTimeoutException("Connect to " + remoteAddress.getHostName() + "/"
						+ remoteAddress.getAddress() + " timed out");
			}
			return sock;
		}

		@Override
		public Socket createSocket(final HttpParams params) throws IOException {
			if (params == null) {
				throw new IllegalArgumentException("HTTP parameters may not be null");
			}

			InetSocketAddress socksaddr = new InetSocketAddress(proxyHost, proxyPort);
			Proxy proxy = new Proxy(Proxy.Type.SOCKS, socksaddr);
			return new Socket(proxy);
		}

		@Override
		public boolean isSecure(final Socket sock) throws IllegalArgumentException {
			return false;
		}

	}

	public static final String DEFAULT_CLIENT = "direct-http";

	private final Map<String, AbstractHttpClient> clients;

	private String localSocksPorts;

	public HttpManager() {

		clients = new LinkedHashMap<String, AbstractHttpClient>();
	}

	@PostConstruct
	public void afterPropertiesSet() {
		{
			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", 80, PlainSocketFactory.getSocketFactory()));
			registry.register(new Scheme("https", 443, SSLSocketFactory.getSocketFactory()));

			HttpParams connectionParameters = new BasicHttpParams();
			setDefaultHttpClientParams(connectionParameters);
			ThreadSafeClientConnManager clientConnManager = new ThreadSafeClientConnManager(connectionParameters,
					registry);
			clientConnManager.setDefaultMaxPerRoute(2);
			clientConnManager.setMaxTotal(10);

			DefaultHttpClient client = new DefaultHttpClient(clientConnManager);
			setDefaultHttpClientParams(client.getParams());

			clients.put(DEFAULT_CLIENT, client);
		}

		for (String localpost : StringUtils.split(StringUtils.trimToEmpty(localSocksPorts), " \t\r\n;,")) {
			SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", 80, new SocksClientConnManager("localhost", 1080)));

			HttpParams connectionParameters = new BasicHttpParams();
			setDefaultHttpClientParams(connectionParameters);
			ThreadSafeClientConnManager clientConnManager = new ThreadSafeClientConnManager(connectionParameters,
					registry);
			clientConnManager.setDefaultMaxPerRoute(2);
			clientConnManager.setMaxTotal(10);

			DefaultHttpClient client = new DefaultHttpClient(clientConnManager);
			setDefaultHttpClientParams(client.getParams());

			clients.put("SOCKS (" + localpost + ")", client);
		}
	}

	public HttpResponse execute(String clientCode, HttpUriRequest request) throws IOException, ClientProtocolException {
		return getClient(clientCode).execute(request);
	}

	public <T> T execute(String clientCode, HttpUriRequest request, ResponseHandler<? extends T> responseHandler)
			throws IOException, ClientProtocolException {
		return getClient(clientCode).execute(request, responseHandler);
	}

	public HttpResponse executeFromLocalhost(HttpUriRequest request) throws IOException, ClientProtocolException {
		return execute(DEFAULT_CLIENT, request);
	}

	public <T> T executeFromLocalhost(HttpUriRequest request, ResponseHandler<? extends T> responseHandler)
			throws IOException, ClientProtocolException {
		return execute(DEFAULT_CLIENT, request, responseHandler);
	}

	public AbstractHttpClient getClient(String clientCode) {
		return clients.get(clientCode);
	}

	public Set<String> getClientCodes() {
		return Collections.unmodifiableSet(clients.keySet());
	}

	public AbstractHttpClient getLocalhostClient() {
		return clients.get(DEFAULT_CLIENT);
	}

	@Transactional(propagation = Propagation.SUPPORTS)
	public CookieStore getLocalhostCookieStore() {
		return getLocalhostClient().getCookieStore();
	}

	public String getLocalSocksPorts() {
		return localSocksPorts;
	}

	public void setDefaultHttpClientParams(final HttpParams clientParams) {
		HttpClientParams.setRedirecting(clientParams, true);
		HttpProtocolParams.setUserAgent(clientParams, "Secretary/JWBF");
		HttpConnectionParams.setConnectionTimeout(clientParams, 60 * 1000);
		HttpConnectionParams.setSoTimeout(clientParams, 5 * 60 * 1000);
	}

	public void setLocalSocksPorts(String localSocksPorts) {
		this.localSocksPorts = localSocksPorts;
	}

}
