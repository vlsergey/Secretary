package org.wikipedia.vlsergey.secretary.http;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

public class ExternalIpChecker {

	private static final Logger logger = LoggerFactory.getLogger(ExternalIpChecker.class);

	@Autowired
	private HttpManager httpManager;

	public void assertIpAddressesAreDifferent() throws ClientProtocolException, IOException {
		if (httpManager.getClientCodes().size() == 1)
			return;

		Set<InetAddress> usedAddresses = new LinkedHashSet<InetAddress>();

		for (String clientCode : httpManager.getClientCodes()) {
			InetAddress inetAddress = httpManager.execute(clientCode, new HttpGet("http://2ip.ru/"),
					new ResponseHandler<InetAddress>() {
						@Override
						public InetAddress handleResponse(HttpResponse response) throws ClientProtocolException,
								IOException {

							InputStream inputStream = response.getEntity().getContent();
							String contentEncoding = response.getEntity().getContentEncoding() != null ? response
									.getEntity().getContentEncoding().getValue() : "";
							if ("gzip".equalsIgnoreCase(contentEncoding))
								inputStream = new GZIPInputStream(inputStream);

							String encoding = StringUtils.substringAfter(response.getEntity().getContentType()
									.getValue(), "charset=");
							String out;
							if (StringUtils.isNotEmpty(encoding)) {
								out = IoUtils.readToString(inputStream, encoding);
							} else {
								out = IoUtils.readToString(inputStream, "utf-8");
							}

							String ip = out;
							ip = StringUtils.substringAfter(ip, "			</span> <big>");
							ip = StringUtils.substringBefore(ip, "</big>");
							InetAddress inetAddress = InetAddress.getByName(ip);
							return inetAddress;
						}
					});
			logger.info("External IP of client connection '" + clientCode + "' is " + inetAddress);
			if (!usedAddresses.add(inetAddress)) {
				throw new AssertionError("Some other client connection already used external IP " + inetAddress);
			}
		}
	}

	@PostConstruct
	public void init() throws Exception {
		assertIpAddressesAreDifferent();
	}
}
