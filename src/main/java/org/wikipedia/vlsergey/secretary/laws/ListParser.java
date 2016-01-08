package org.wikipedia.vlsergey.secretary.laws;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.AbstractHttpClient;
import org.wikipedia.vlsergey.secretary.http.BasicResponseHandler;
import org.wikipedia.vlsergey.secretary.http.HttpManager;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

public class ListParser {
	public static void main(String[] args) throws Exception {
		new ListParser().parse();
	}

	private void parse() throws Exception {
		HttpManager httpManager = new HttpManager();
		httpManager.afterPropertiesSet();
		final AbstractHttpClient client = httpManager.getClient(HttpManager.DEFAULT_CLIENT);

		client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

		String html = IoUtils.readToString(PageParser.class.getResourceAsStream("list.html"), "Windows-1251");
		for (String docId : StringUtils.substringsBetween(html, "\" pid=\"", "\" ")) {

			final HttpGet request = new HttpGet("http://text.document.kremlin.ru/SESSION/PILOT/main.htm");
			httpManager.execute(HttpManager.DEFAULT_CLIENT, request, new BasicResponseHandler("Windows-1251"));

			final HttpGet request2 = new HttpGet("http://text.document.kremlin.ru/SESSION/PILOT/main.htm");
			request2.addHeader("Referer", "http://text.document.kremlin.ru/SESSION/PILOT/logout.htm");
			httpManager.execute(HttpManager.DEFAULT_CLIENT, request2, new BasicResponseHandler("Windows-1251"));

			final HttpGet request3 = new HttpGet(
					"http://text.document.kremlin.ru/SESSION/PILOT/doc/doc_print.html?print_type=1&pid=" + docId
							+ "&blockPointer=0&garantCommentsOn=0");
			request3.addHeader("Referer", "http://text.document.kremlin.ru/SESSION/PILOT/main.htm");

			String result = client.execute(request3, new BasicResponseHandler("Windows-1251"));

			System.out.println(result);
		}
	}
}
