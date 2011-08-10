package org.wikipedia.vlsergey.secretary.webcite;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
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
import org.wikipedia.vlsergey.secretary.dom.Parameter;
import org.wikipedia.vlsergey.secretary.dom.Template;
import org.wikipedia.vlsergey.secretary.http.HttpManager;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;

public class WebCiteArchiver {

	private static final Log logger = LogFactory.getLog(WebCiteArchiver.class);

	private static final String PATTERN_WEBCITE_ARCHIVE_RESPONSE = "An[ ]archive[ ]of[ ]this[ ]page[ ]should[ ]shortly[ ]be[ ]available[ ]at[ ]\\<\\/p\\>\\<br[ ]\\/\\>"
			+ "\\<p\\>\\<a[ ]href\\=([^\\>]*)\\>";
	private static final String PATTERN_WEBCITE_QUERY_RESPONSE = "\\<resultset\\>\\<result status\\=\\\"([^\\\"]*)\\\"\\>";

	boolean ignoreCite(PerArticleReport perArticleReport,
			Template citeWebTemplate) {

		Parameter urlParameter = citeWebTemplate
				.getParameter(WikiConstants.PARAMETER_URL);
		if (urlParameter == null
				|| StringUtils.isEmpty(urlParameter.getValue().toString()
						.trim()))
			return true;
		String url = urlParameter.getValue().toString().trim();

		Parameter deadlinkParameter = citeWebTemplate
				.getParameter(WikiConstants.PARAMETER_DEADLINK);
		if (deadlinkParameter != null
				&& StringUtils.isNotEmpty(deadlinkParameter.getValue()
						.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedDead(url);

			return true;
		}

		Parameter archiveurlParameter = citeWebTemplate
				.getParameter(WikiConstants.PARAMETER_ARCHIVEURL);
		if (archiveurlParameter != null
				&& StringUtils.isNotEmpty(archiveurlParameter.getValue()
						.toString().trim())) {

			if (perArticleReport != null)
				perArticleReport.skippedMarkedArchived(url);

			return true;
		}

		if (ignoreUrl(perArticleReport, url))
			return true;

		return false;
	}

	static boolean ignoreUrl(PerArticleReport perArticleReport, String url) {
		if (!url.startsWith("http://"))
			return true;

		URI uri;
		try {
			uri = URI.create(url);
		} catch (IllegalArgumentException exc) {
			logger.warn("URL " + url + " skipped due wrong format: "
					+ exc.getMessage());

			if (perArticleReport != null)
				perArticleReport.skippedIncorrectFormat(url);

			return true;
		}

		String host = uri.getHost().toLowerCase();

		return WebCiteArchiver.isIgnoreHost(perArticleReport, url, host);
	}

	static final Set<String> SKIP_ERRORS = new HashSet<String>(Arrays.asList(
	//

			// http://www.webcitation.org/5wAZdFTwc
			"beyond2020.cso.ie",

			// http://www.webcitation.org/5w7BcNTfc
			"logainm.ie",

			// http://www.webcitation.org/5w7BcNTfc
			"www.logainm.ie"

	));

	static final Set<String> SKIP_NO_CACHE = new HashSet<String>(Arrays.asList(
	//
			"www1.folha.uol.com.br", //

			"www.ctv.ca",//

			"www.bluesnews.com",//
			"www.inishturkisland.com",//
			"www.janes.com",//
			"www.plastichead.com",//
			"www.sherdog.com",//
			"securitylabs.websense.com",//

			"www.sportovci.cz",//

			"antiaircraft.org",//

			"www.3dnews.ru",//
			"cult.compulenta.ru",//
			"hard.compulenta.ru",//
			"offline.computerra.ru",//
			"www.computerra.ru",//
			"www.crpg.ru",//
			"www.dishmodels.ru",//
			"www.game-ost.ru",//
			"interfax.ru", "www.interfax.ru", //
			"www.tver.izbirkom.ru",//
			"www.liveinternet.ru",//
			"kino.otzyv.ru",//
			"www.rg.ru",//
			"www.systematic.ru",//
			"www.translogist.ru",//

			"media.mabila.ua"//

	));

	static final Set<String> SKIP_ARCHIVES = new HashSet<String>(Arrays.asList(
			//
			"archive.wikiwix.com", "wikiwix.com",

			"classic-web.archive.org", "liveweb.archive.org",
			"replay.web.archive.org", "web.archive.org",

			"liveweb.waybackmachine.org", "replay.waybackmachine.org",

			"webcitation.org", "www.webcitation.org",

			"www.peeep.us"));

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
					"www.stpattys.com",//
					"www.wheresgeorge.com",//
					"ru.youtube.com",//
					"www.youtube.com",//

					"futuretrance.de",//
					"www.rfid-handbook.de",//
					"www.voicesfromthedarkside.de",//

					"voynich.nu",//

					"www.file-extensions.org",//
					"www.globalsecurity.org",//
					"www.mindat.org",//
					"wwww.spatricksf.org",//
					"www.solon.org",//
					"www.yellowribbon.org",//

					"books.google.ru",//
					"video.mail.ru",//
					"www.ozon.ru",//
					"really.ru",//
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

	static boolean isIgnoreHost(PerArticleReport perArticleReport, String url,
			String host) {

		if (SKIP_ERRORS.contains(host)) {
			logger.debug("URL "
					+ url
					+ " skipped due to usual errors of WebCite leading to undeadable text");

			if (perArticleReport != null)
				perArticleReport.skippedIgnoreTechLimits(url);

			return true;
		}

		if (SKIP_TECH_LIMITS.contains(host)) {
			logger.debug("URL " + url
					+ " skipped due to technical limitatios of WebCite");

			if (perArticleReport != null)
				perArticleReport.skippedIgnoreTechLimits(url);

			return true;
		}

		if (SKIP_NO_CACHE.contains(host)) {
			logger.debug("URL "
					+ url
					+ " skipped because pages on this site usually have 'no-cache' tag");

			if (perArticleReport != null)
				perArticleReport.skippedIgnoreNoCache(url);

			return true;
		}

		if (SKIP_ARCHIVES.contains(host)) {
			logger.debug("URL " + url + " skipped (are u serious?)");

			if (perArticleReport != null)
				perArticleReport.skippedIgnoreSence(url);

			return true;
		}

		return false;
	}

	@Autowired
	private HttpManager httpManager;

	public String archive(final String url, final String title,
			final String author, final String date) throws Exception {

		// okay, archiving
		logger.debug("Archiving " + url);

		HttpPost httpPost = buildRequest(url, title, author, date);

		return httpManager.execute(httpPost, new ResponseHandler<String>() {
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
				logger.info("URL " + url + " was archived at " + archiveUrl);
				return archiveUrl;
			}
		});
	}

	public String getStatus(final String webCiteId)
			throws ClientProtocolException, IOException {
		HttpGet getMethod = new HttpGet(
				"http://www.webcitation.org/query?returnxml=true&id="
						+ webCiteId);

		return httpManager.execute(getMethod, new ResponseHandler<String>() {
			public String handleResponse(HttpResponse httpResponse)
					throws ClientProtocolException, IOException {

				HttpEntity entity = httpResponse.getEntity();
				String result = IoUtils.readToString(entity.getContent(),
						HTTP.UTF_8);

				Pattern pattern = Pattern
						.compile(PATTERN_WEBCITE_QUERY_RESPONSE);
				Matcher matcher = pattern.matcher(result);

				if (matcher.find()) {

					logger.debug("Archive status of '" + webCiteId + "' is '"
							+ matcher.group(1) + "'");

					return matcher.group(1);
				}

				if (!matcher.find()) {
					logger.error("Pattern of response not found on query XML response page");
					logger.trace(result);
				}

				return null;
			}
		});

	}

}
