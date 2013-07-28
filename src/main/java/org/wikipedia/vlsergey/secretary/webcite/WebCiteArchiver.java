package org.wikipedia.vlsergey.secretary.webcite;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.http.HttpManager;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

public class WebCiteArchiver {

	private static final Log logger = LogFactory.getLog(WebCiteArchiver.class);

	private static final String PATTERN_WEBCITE_ARCHIVE_RESPONSE = "An[ ]archive[ ]of[ ]this[ ]page[ ]should[ ]shortly[ ]be[ ]available[ ]at[ ]\\<\\/p\\>\\<br[ ]\\/\\>"
			+ "\\<p\\>\\<a[ ]href\\=([^\\>]*)\\>";
	private static final String PATTERN_WEBCITE_QUERY_RESPONSE = "\\<resultset\\>\\<result status\\=\\\"([^\\\"]*)\\\"\\>";

	private static HttpPost buildRequest(final String url, final String title, final String author, final String date)
			throws UnsupportedEncodingException, IOException, ClientProtocolException {

		HttpPost postMethod = new HttpPost("http://webcitation.org/archive.php");

		List<NameValuePair> nvps = new ArrayList<NameValuePair>();
		nvps.add(new BasicNameValuePair("url", url));
		nvps.add(new BasicNameValuePair("email", "vlsergey@gmail.com"));
		nvps.add(new BasicNameValuePair("title", title));
		nvps.add(new BasicNameValuePair("author", author));
		nvps.add(new BasicNameValuePair("authoremail", ""));
		nvps.add(new BasicNameValuePair("source", ""));
		nvps.add(new BasicNameValuePair("date", date));
		nvps.add(new BasicNameValuePair("subject", ""));
		nvps.add(new BasicNameValuePair("fromform", "1"));
		nvps.add(new BasicNameValuePair("submit", "Submit"));

		postMethod.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
		return postMethod;
	}

	@Autowired
	private HttpManager httpManager;

	public String archive(final String httpClientCode, final String url, final String title, final String author,
			final String date) throws Exception {

		// okay, archiving
		logger.debug("Using " + httpClientCode + " to archive " + url);

		HttpPost httpPost = buildRequest(url, title, author, date);

		return httpManager.execute(httpClientCode, httpPost, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse archiveResponse) throws ClientProtocolException, IOException {

				if (archiveResponse.getStatusLine().getStatusCode() != 200) {
					logger.error("Unsupported response: " + archiveResponse.getStatusLine());
					throw new UnsupportedOperationException("Unsupported response code from WebCite");
				}

				String result = IoUtils.readToString(archiveResponse.getEntity().getContent(), HTTP.UTF_8);

				Pattern pattern = Pattern.compile(PATTERN_WEBCITE_ARCHIVE_RESPONSE);
				Matcher matcher = pattern.matcher(result);

				if (!matcher.find()) {
					logger.error("Pattern of response not found on archiving response page");
					logger.debug(result);

					throw new UnsupportedOperationException("Unsupported from response content. "
							+ "Details in DEBUG log.");
				}

				String archiveUrl = matcher.group(1);
				logger.info("URL " + url + " was archived at " + archiveUrl);
				return archiveUrl;
			}
		});
	}

	public String getStatus(final String httpClientCode, final String webCiteId) throws ClientProtocolException,
			IOException {
		HttpGet getMethod = new HttpGet("http://www.webcitation.org/query?returnxml=true&id=" + webCiteId);

		return httpManager.execute(httpClientCode, getMethod, new ResponseHandler<String>() {
			@Override
			public String handleResponse(HttpResponse httpResponse) throws ClientProtocolException, IOException {

				HttpEntity entity = httpResponse.getEntity();
				String result = IoUtils.readToString(entity.getContent(), HTTP.UTF_8);

				Pattern pattern = Pattern.compile(PATTERN_WEBCITE_QUERY_RESPONSE);
				Matcher matcher = pattern.matcher(result);

				if (matcher.find()) {

					logger.debug("Archive status of '" + webCiteId + "' is '" + matcher.group(1) + "'");

					return matcher.group(1);
				}

				if (result.contains("<error>Invalid snapshot ID "))
					return "Invalid snapshot ID";

				if (!matcher.find()) {
					logger.error("Pattern of response not found on query XML response page for ID '" + webCiteId + "'");
					logger.trace(result);
				}

				return null;
			}
		});

	}

}
