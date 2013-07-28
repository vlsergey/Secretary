package org.wikipedia.vlsergey.secretary.webcite.lists;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class TechLimits extends SkipList {

	static final Set<String> HOSTS = new HashSet<String>(Arrays.asList(
	//

			"armsport.am", // 404

			"naviny.by", // 404

			"animenewsnetwork.com",//
			"azlyrics.com",//
			"boston.com",//
			"cinnamonpirate.com",//
			"city-data.com", // 404
			"discogs.com",//
			"dpreview.com", //
			"encyclopedia.com", // 400 (bad request)
			"everyculture.com",//
			"facebook.com", // unknown
			"filmreference.com", // 404
			"findarticles.com",//
			"aom.heavengames.com", // 404
			"historynet.com", // 404
			"intel.com", // 404
			"forum.ixbt.com",//
			"gamespot.com", // 404
			"groups.google.com", "news.google.com",//
			"jame-world.com",//
			"tests.jeuxmac.com", // incorrectly returns 404
			"london2012.com", // 403
			"nationsencyclopedia.com", // 404
			"ttcs.netfirms.com", // long timeout
			"oceandots.com", // 404
			"pqasb.pqarchiver.com", // 404
			"rottentomatoes.com", //
			"sciencedirect.com", // 404
			"slantmagazine.com", // 404
			"springerlink.com", // 404
			"stpattys.com",//
			"rogerebert.suntimes.com", // 404
			"visi.com",//
			"webelements.com",//
			"wheresgeorge.com",//

			"biolib.cz", // 404

			"futuretrance.de",//
			"rfid-handbook.de",//
			"structurae.de", // 404
			"voicesfromthedarkside.de",//

			"zapraudu-mirror.info", // 404

			"earthobservatory.nasa.gov", // 404
			"ncbi.nlm.nih.gov",//
			"ncdc.noaa.gov", // 404

			"voynich.nu",//

			"aerospaceweb.org",//
			"file-extensions.org", //
			"globalsecurity.org",//
			"hdot.org", //
			"iaea.org", // 404
			"mindat.org", //
			"spatricksf.org", //
			"solon.org", //
			"portal.unesco.org", //
			"unhcr.org", //
			"yellowribbon.org",//

			"ag.ru", // 404
			"championat.ru", // 404
			"computer-museum.ru", // 404
			"fantlab.ru", // 404
			"encspb.ru", // 404
			"gasur.ru", // 404
			"grwar.ru", // 404
			"nkj.ru", // 404
			"ozon.ru",//
			"really.ru",//
			"perm.ru", // 404
			"spartak-nalchik.ru", // 404
			"videoguide.ru", // 404
			"walkspb.ru", // 404

			"ati.su", //

			"www.google.com.ua",//

			"jl.sl.btinternet.co.uk", // 404
			"thesun.co.uk", //
			"timesonline.co.uk", // 404
			"traditionalmusic.co.uk",

			"-"//
	));

	public static boolean contains(URI toTest) {
		return contains(HOSTS, toTest);
	}

}
