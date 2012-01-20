package org.wikipedia.vlsergey.secretary.webcite;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
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
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

public class WebCiteArchiver {

	private static final Log logger = LogFactory.getLog(WebCiteArchiver.class);

	private static final String PATTERN_WEBCITE_ARCHIVE_RESPONSE = "An[ ]archive[ ]of[ ]this[ ]page[ ]should[ ]shortly[ ]be[ ]available[ ]at[ ]\\<\\/p\\>\\<br[ ]\\/\\>"
			+ "\\<p\\>\\<a[ ]href\\=([^\\>]*)\\>";
	private static final String PATTERN_WEBCITE_QUERY_RESPONSE = "\\<resultset\\>\\<result status\\=\\\"([^\\\"]*)\\\"\\>";

	static final Set<String> SKIP_ARCHIVES = new HashSet<String>(Arrays.asList(
			//
			"archive.wikiwix.com", "wikiwix.com",

			"classic-web.archive.org", "liveweb.archive.org",
			"replay.web.archive.org", "web.archive.org",

			"liveweb.waybackmachine.org", "replay.waybackmachine.org",

			"webcitation.org", "www.webcitation.org",

			"www.peeep.us"));

	static final Set<String> SKIP_ERRORS = new HashSet<String>(Arrays.asList(
	//

			// http://www.webcitation.org/5wAZdFTwc
			"beyond2020.cso.ie",

			// http://www.webcitation.org/5w7BcNTfc
			"logainm.ie",

			// http://www.webcitation.org/5w7BcNTfc
			"www.logainm.ie"));

	static final Set<String> SKIP_NO_CACHE = new HashSet<String>(Arrays.asList(
			//
			"www1.folha.uol.com.br", //

			"www.ctv.ca",//

			"www.bluesnews.com",//
			"www.inishturkisland.com",//
			"www.janes.com",//
			"ms-pictures.com",
			"www.ms-pictures.com", //
			"www.plastichead.com",//
			"www.sherdog.com",//
			"secunia.com",
			"www.secunia.com",//
			"securitylabs.websense.com",//

			"www.sportovci.cz",//

			"blogs.yahoo.co.jp", //

			"antiaircraft.org",//

			"www.3dnews.ru",//
			"cult.compulenta.ru", "hard.compulenta.ru",
			"science.compulenta.ru",//
			"offline.computerra.ru", "www.computerra.ru",//
			"www.crpg.ru",//
			"www.dishmodels.ru",//
			"domtest.ru",//
			"www.game-ost.ru",//
			"infuture.ru", "www.infuture.ru", //
			"interfax.ru", "www.interfax.ru", //
			"www.tver.izbirkom.ru",//
			"www.liveinternet.ru",//
			"astro-era.narod.ru", //
			"kino.otzyv.ru",//
			"render.ru", "www.render.ru", //
			"www.rg.ru",//
			"www.systematic.ru",//
			"www.translogist.ru",//

			"media.mabila.ua"//

	));

	static final Set<String> SKIP_TECH_LIMITS = new HashSet<String>(
			Arrays.asList(
			//
					"books.google.com.br",//

					"www.animenewsnetwork.com",//
					"www.azlyrics.com",//
					"cinnamonpirate.com",//
					"www.discogs.com",//
					"books.google.com",//
					"forum.ixbt.com",//
					"nationsencyclopedia.com", "www.nationsencyclopedia.com", //
					"www.stpattys.com",//
					"www.wheresgeorge.com",//
					"ru.youtube.com",//
					"www.youtube.com",//

					"futuretrance.de",//
					"books.google.de",//
					"www.rfid-handbook.de",//
					"www.voicesfromthedarkside.de",//

					"voynich.nu",//

					"www.file-extensions.org",//
					"www.globalsecurity.org",//
					"www.mindat.org",//
					"wwww.spatricksf.org",//
					"www.solon.org",//
					"www.yellowribbon.org",//

					"computer-museum.ru", "www.computer-museum.ru", // alw404
					"books.google.ru",//
					"video.mail.ru",//
					"www.ozon.ru",//
					"really.ru",//
					"perm.ru", "www.perm.ru", // alw404
					"maps.yandex.ru",//

					"books.google.co.uk",//
					"www.traditionalmusic.co.uk"

			));

	private static HttpPost buildRequest(final String url, final String title,
			final String author, final String date)
			throws UnsupportedEncodingException, IOException,
			ClientProtocolException {

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

	@Autowired
	private MediaWikiBot mediaWikiBot;

	public String archive(final String httpClientCode, final String url,
			final String title, final String author, final String date)
			throws Exception {

		// okay, archiving
		logger.debug("Using " + httpClientCode + " to archive " + url);

		HttpPost httpPost = buildRequest(url, title, author, date);

		return httpManager.execute(httpClientCode, httpPost,
				new ResponseHandler<String>() {
					@Override
					public String handleResponse(HttpResponse archiveResponse)
							throws ClientProtocolException, IOException {

						if (archiveResponse.getStatusLine().getStatusCode() != 200) {
							logger.error("Unsupported response: "
									+ archiveResponse.getStatusLine());
							throw new UnsupportedOperationException(
									"Unsupported response code from WebCite");
						}

						String result = IoUtils.readToString(archiveResponse
								.getEntity().getContent(), HTTP.UTF_8);

						Pattern pattern = Pattern
								.compile(PATTERN_WEBCITE_ARCHIVE_RESPONSE);
						Matcher matcher = pattern.matcher(result);

						if (!matcher.find()) {
							logger.error("Pattern of response not found on archiving response page");
							logger.debug(result);

							throw new UnsupportedOperationException(
									"Unsupported from response content. "
											+ "Details in DEBUG log.");
						}

						String archiveUrl = matcher.group(1);
						logger.info("URL " + url + " was archived at "
								+ archiveUrl);
						return archiveUrl;
					}
				});
	}

	public String getStatus(final String httpClientCode, final String webCiteId)
			throws ClientProtocolException, IOException {
		HttpGet getMethod = new HttpGet(
				"http://www.webcitation.org/query?returnxml=true&id="
						+ webCiteId);

		return httpManager.execute(httpClientCode, getMethod,
				new ResponseHandler<String>() {
					@Override
					public String handleResponse(HttpResponse httpResponse)
							throws ClientProtocolException, IOException {

						HttpEntity entity = httpResponse.getEntity();
						String result = IoUtils.readToString(
								entity.getContent(), HTTP.UTF_8);

						Pattern pattern = Pattern
								.compile(PATTERN_WEBCITE_QUERY_RESPONSE);
						Matcher matcher = pattern.matcher(result);

						if (matcher.find()) {

							logger.debug("Archive status of '" + webCiteId
									+ "' is '" + matcher.group(1) + "'");

							return matcher.group(1);
						}

						if (result.contains("<error>Invalid snapshot ID "))
							return "Invalid snapshot ID";

						if (!matcher.find()) {
							logger.error("Pattern of response not found on query XML response page for ID '"
									+ webCiteId + "'");
							logger.trace(result);
						}

						return null;
					}
				});

	}

	public void updateIgnoringList() throws Exception {
		updateIgnoringList(SKIP_ERRORS,
				"Участник:WebCite Archiver/IgnoreErrors");
		updateIgnoringList(SKIP_NO_CACHE,
				"Участник:WebCite Archiver/IgnoreNoCache");
		updateIgnoringList(SKIP_ARCHIVES,
				"Участник:WebCite Archiver/IgnoreSence");
		updateIgnoringList(SKIP_TECH_LIMITS,
				"Участник:WebCite Archiver/IgnoreTechLimits");
	}

	private void updateIgnoringList(Set<String> hostsToIgnore, String pageName)
			throws Exception {
		StringBuffer stringBuffer = new StringBuffer();

		List<String> hosts = new ArrayList<String>(hostsToIgnore);
		Collections.sort(hosts, new Comparator<String>() {

			final Map<String, String> cache = new HashMap<String, String>();

			@Override
			public int compare(String o1, String o2) {

				String s1 = inverse(o1);
				String s2 = inverse(o2);

				return s1.compareToIgnoreCase(s2);
			}

			private String inverse(String direct) {
				String result = cache.get(direct);
				if (result != null)
					return result;

				String[] splitted = StringUtils.split(direct, ".");
				Collections.reverse(Arrays.asList(splitted));
				result = StringUtils.join(splitted, ".");
				cache.put(direct, result);
				return result;
			}
		});

		for (String hostName : hosts) {
			stringBuffer.append("* " + hostName + "\n");
		}

		mediaWikiBot.writeContent(pageName, null, stringBuffer.toString(),
				null, "Update ignoring sites list", true, true, false);
	}

}
