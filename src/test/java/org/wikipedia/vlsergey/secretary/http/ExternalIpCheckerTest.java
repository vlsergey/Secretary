package org.wikipedia.vlsergey.secretary.http;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ExternalIpCheckerTest {

	@Test
	public void testAssertIpAddressesAreDifferent()
			throws ClientProtocolException, IOException {
		ApplicationContext appContext = new ClassPathXmlApplicationContext(
				"application-context.xml");

		ExternalIpChecker externalIpChecker = appContext
				.getBean(ExternalIpChecker.class);
		externalIpChecker.assertIpAddressesAreDifferent();
	}

}
