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

			"classic-web.archive.org", "liveweb.archive.org", "replay.web.archive.org", "web.archive.org",

			"liveweb.waybackmachine.org", "replay.waybackmachine.org",

			"webcitation.org", "www.webcitation.org",

			"en.wikisource.org", "ru.wikisource.org",

			"www.peeep.us"));

	/**
	 * This URL has been archived internally and can be made available for
	 * scholars on request, but we cannot make it accessible on the web, because
	 * the copyright holder (...) has asked us not to display the material. If
	 * you have concerns about this individual not being the copyright holder,
	 * or if you require access to the material in our dark archive for
	 * scholarly or legal purposes, please contact us.
	 */
	static final Set<String> SKIP_BLACKLISTED = new HashSet<String>(Arrays.asList(
	//
			"timeshighereducation.co.uk", "www.timeshighereducation.co.uk"));

	static final Set<String> SKIP_ERRORS = new HashSet<String>(Arrays.asList(
	//

			// http://www.webcitation.org/5w563Hk2c
			"billboard.com", "www.billboard.com",

			// http://content.yudu.com/Library/A1ntfz/ITFAnnualReportAccou/resources/index.htm?referrerUrl=
			"content.yudu.com",

			// http://www.webcitation.org/5wAZdFTwc
			"beyond2020.cso.ie",

			// http://www.webcitation.org/5w7BcNTfc
			"logainm.ie",

			// http://www.webcitation.org/5w7BcNTfc
			"www.logainm.ie",

			// always 403 by HTTP checker
			"euskomedia.org", "www.euskomedia.org"

	));

	static final Set<String> SKIP_NO_CACHE = new HashSet<String>(Arrays.asList(
			//
			"www1.folha.uol.com.br", //

			"www.ctv.ca",//

			"bluesnews.com",
			"www.bluesnews.com",//
			"chelseafc.com",
			"www.chelseafc.com",//
			"dlib.eastview.com",//
			"inishturkisland.com",
			"www.inishturkisland.com",//
			"janes.com",
			"www.janes.com",//
			"ms-pictures.com",
			"www.ms-pictures.com", //
			"movies.nytimes.com",//
			"plastichead.com",
			"www.plastichead.com",//
			"sherdog.com",
			"www.sherdog.com",//
			"secunia.com",
			"www.secunia.com",//
			"securitylabs.websense.com",//
			"whufc.com",
			"www.whufc.com",//
			"worldsnooker.com",
			"www.worldsnooker.com",//
			"x-rates.com",
			"www.x-rates.com",//
			"xbiz.com",
			"www.xbiz.com", //

			"ifpicr.cz",
			"www.ifpicr.cz", //
			"sportovci.cz",
			"www.sportovci.cz",//

			"www.nationalbanken.dk",//

			"blogs.yahoo.co.jp", //

			"fff.fr",
			"www.fff.fr",//

			"izrus.co.il",//

			"www.groklaw.net",//
			"zonakz.net",
			"www.zonakz.net",//

			"antiaircraft.org",//
			"paclii.org",
			"www.paclii.org",//
			"rfemmr.org",//

			"www.3dnews.ru",//
			"www.art-catalog.ru",//
			"www.cio-world.ru",//
			"compulenta.ru", "business.compulenta.ru", "cult.compulenta.ru", "culture.compulenta.ru",
			"hard.compulenta.ru", "games.compulenta.ru", "net.compulenta.ru",
			"science.compulenta.ru",
			"soft.compulenta.ru",//
			"computerra.ru", "offline.computerra.ru",
			"www.computerra.ru",//
			"www.crpg.ru",//
			"www.dishmodels.ru",//
			"domtest.ru",//
			"www.finam.ru",//
			"finmarket.ru",
			"www.finmarket.ru",//
			"game-ost.ru",
			"www.game-ost.ru",//
			"gatchina-meria.ru",
			"www.gatchina-meria.ru",//
			"glossary.ru",
			"www.glossary.ru",//
			"infuture.ru",
			"www.infuture.ru", //
			"interfax.ru",
			"www.interfax.ru", //
			"interfax-russia.ru",
			"www.interfax-russia.ru", //
			"tver.izbirkom.ru", "www.tver.izbirkom.ru", "vybory.izbirkom.ru", "volgograd.vybory.izbirkom.ru",
			"www.volgograd.vybory.izbirkom.ru", "www.vybory.izbirkom.ru",//
			"graph.document.kremlin.ru",//
			"liveinternet.ru",//
			"www.liveinternet.ru",//
			"mountain.ru", "www.mountain.ru",//
			"astro-era.narod.ru", //
			"newsmusic.ru", "www.newsmusic.ru",//
			"kino.otzyv.ru",//
			"oval.ru", "www.oval.ru",//
			"redstar.ru", "www.redstar.ru",//
			"render.ru", "www.render.ru", //
			"rg.ru", "www.rg.ru",//
			"ruformator.ru", //
			"scrap-info.ru", //
			"soccer.ru", "www.soccer.ru", //
			"systematic.ru", "www.systematic.ru",//
			"translogist.ru", "www.translogist.ru",//
			"webapteka.ru", "www.webapteka.ru",//

			"zakon.rada.gov.ua", //
			"zakon1.rada.gov.ua", //
			"media.mabila.ua",//

			"nufc.co.uk", "www.nufc.co.uk", //
			"cajt.pwp.blueyonder.co.uk", "www.cajt.pwp.blueyonder.co.uk" //
	));

	static final Set<String> SKIP_TECH_LIMITS = new HashSet<String>(Arrays.asList(
	//

			"armsport.am", "www.armsport.am", // 404

			"books.google.com.br",//

			"naviny.by", "www.naviny.by", // 404

			"www.animenewsnetwork.com",//
			"www.azlyrics.com",//
			"www.boston.com",//
			"cinnamonpirate.com",//
			"city-data.com", "www.city-data.com", // 404
			"www.discogs.com",//
			"dpreview.com", "www.dpreview.com", //
			"encyclopedia.com", "www.encyclopedia.com", // 400 (bad request)
			"www.everyculture.com",//
			"facebook.com", "www.facebook.com", // unknown
			"filmreference.com", "www.filmreference.com", // 404
			"findarticles.com",//
			"aom.heavengames.com", // 404
			"historynet.com", "www.historynet.com",// 404
			"intel.com", "www.intel.com", // 404
			"forum.ixbt.com",//
			"gamespot.com", "www.gamespot.com", // 404
			"books.google.com", "groups.google.com", "news.google.com",//
			"www.jame-world.com",//
			"tests.jeuxmac.com", // incorrectly returns 404
			"london2012.com", "www.london2012.com", // 403
			"nationsencyclopedia.com", "www.nationsencyclopedia.com", // 404
			"ttcs.netfirms.com", // long timeout
			"oceandots.com", "www.oceandots.com", // 404
			"pqasb.pqarchiver.com", // 404
			"rottentomatoes.com", "www.rottentomatoes.com",//
			"www.sciencedirect.com", // 404
			"slantmagazine.com", "www.slantmagazine.com", // 404
			"springerlink.com", "www.springerlink.com",// 404
			"www.stpattys.com",//
			"rogerebert.suntimes.com", // 404
			"www.visi.com",//
			"www.webelements.com",//
			"www.wheresgeorge.com",//
			"ru.youtube.com",//
			"www.youtube.com",//

			"biolib.cz", "www.biolib.cz", // 404

			"futuretrance.de",//
			"books.google.de",//
			"www.rfid-handbook.de",//
			"structurae.de", "en.structurae.de", // 404
			"www.voicesfromthedarkside.de",//

			"zapraudu-mirror.info", // 404

			"earthobservatory.nasa.gov", // 404
			"www.ncbi.nlm.nih.gov",//
			"www4.ncdc.noaa.gov", // 404

			"voynich.nu",//

			"aerospaceweb.org", "www.aerospaceweb.org",//
			"arxiv.org", "www.arxiv.org",//
			"file-extensions.org", "www.file-extensions.org",//
			"globalsecurity.org", "www.globalsecurity.org",//
			"hdot.org", "www.hdot.org", //
			"iaea.org", "www.iaea.org", // 404
			"mindat.org", "www.mindat.org",//
			"spatricksf.org", "www.spatricksf.org", "wwww.spatricksf.org",//
			"solon.org", "www.solon.org",//
			"portal.unesco.org", //
			"unhcr.org", "www.unhcr.org", //
			"www.yellowribbon.org",//

			"ag.ru", "www.ag.ru", // 404
			"championat.ru", "www.championat.ru", // 404
			"computer-museum.ru", "www.computer-museum.ru", // 404
			"base.consultant.ru", //
			"fantlab.ru", "www.fantlab.ru", // 404
			"encspb.ru", "www.encspb.ru", // 404
			"gasur.ru", "www.gasur.ru", // 404
			"books.google.ru",//
			"grwar.ru", "www.grwar.ru", // 404
			"video.mail.ru",//
			"www.nkj.ru", // 404
			"www.ozon.ru",//
			"really.ru",//
			"perm.ru", "www.perm.ru", // 404
			"rutube.ru", "www.rutube.ru",// sense
			"zakon.scli.ru", // 404
			"spartak-nalchik.ru", "www.spartak-nalchik.ru", // 404
			"videoguide.ru", "www.videoguide.ru", // 404
			"walkspb.ru", "www.walkspb.ru", // 404
			"maps.yandex.ru",//

			"ati.su", "www.ati.su", //

			"books.google.com.ua", "www.google.com.ua",//

			"www.jl.sl.btinternet.co.uk", // 404
			"books.google.co.uk",//
			"thesun.co.uk", "www.thesun.co.uk", //
			"timesonline.co.uk", "entertainment.timesonline.co.uk", "www.timesonline.co.uk", // 404
			"www.traditionalmusic.co.uk"

	));

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

	private MediaWikiBot mediaWikiBot;

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

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
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

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void updateIgnoringList() throws Exception {
		updateIgnoringList(SKIP_ERRORS, "Участник:WebCite Archiver/IgnoreErrors");
		updateIgnoringList(SKIP_NO_CACHE, "Участник:WebCite Archiver/IgnoreNoCache");
		updateIgnoringList(SKIP_ARCHIVES, "Участник:WebCite Archiver/IgnoreSence");
		updateIgnoringList(SKIP_TECH_LIMITS, "Участник:WebCite Archiver/IgnoreTechLimits");
	}

	private void updateIgnoringList(Set<String> hostsToIgnore, String pageName) throws Exception {
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

		mediaWikiBot.writeContent(pageName, null, stringBuffer.toString(), null, "Update ignoring sites list", true,
				false);
	}

}
