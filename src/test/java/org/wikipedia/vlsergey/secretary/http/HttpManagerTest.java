package org.wikipedia.vlsergey.secretary.http;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class HttpManagerTest {

	@Test
	public void testExecuteStringHttpUriRequest()
			throws ClientProtocolException, IOException {
		ApplicationContext appContext = new ClassPathXmlApplicationContext(
				"application-context.xml");

		HttpManager httpManager = appContext.getBean(HttpManager.class);
		httpManager.execute(httpManager.getClientCodes().iterator().next(),
				new HttpGet("http://ru.wikipedia.org/wiki/Test"));
	}

}
