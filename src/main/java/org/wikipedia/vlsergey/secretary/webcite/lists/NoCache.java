package org.wikipedia.vlsergey.secretary.webcite.lists;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class NoCache extends SkipList {

	static final Set<String> HOSTS = new HashSet<String>(Arrays.asList(
	//
			"folha.uol.com.br", //

			"ctv.ca",//

			"bluesnews.com",//
			"chelseafc.com", //
			"dlib.eastview.com",//
			"100.empas.com",//
			"inishturkisland.com", //
			"janes.com", //
			"ms-pictures.com", //
			"movies.nytimes.com",//
			"plastichead.com", //
			"sherdog.com", //
			"secunia.com", //
			"securitylabs.websense.com",//
			"whufc.com",//
			"worldsnooker.com",//
			"x-rates.com",//
			"xbiz.com", //

			"ifpicr.cz", //
			"sportovci.cz",//

			"nationalbanken.dk",//

			"blogs.yahoo.co.jp", //

			"fff.fr",//

			"izrus.co.il",//

			"groklaw.net",//
			"zonakz.net",//

			"antiaircraft.org",//
			"paclii.org",//
			"rfemmr.org",//

			"3dnews.ru",//
			"art-catalog.ru",//
			"cio-world.ru",//
			"compulenta.ru",//
			"computerra.ru", //
			"www.crpg.ru",//
			"www.dishmodels.ru",//
			"domtest.ru",//
			"finam.ru",//
			"finmarket.ru",//
			"game-ost.ru", //
			"gatchina-meria.ru", //
			"glossary.ru", //
			"infuture.ru", //
			"interfax.ru",//
			"interfax-russia.ru", //
			"vybory.izbirkom.ru",//
			"liveinternet.ru",//
			"mountain.ru",//
			"astro-era.narod.ru", //
			"newsmusic.ru",//
			"kino.otzyv.ru",//
			"oval.ru",//
			"redstar.ru",//
			"render.ru", //
			"rg.ru",//
			"ruformator.ru", //
			"scrap-info.ru", //
			"soccer.ru", //
			"systematic.ru",//
			"translogist.ru",//
			"webapteka.ru",//

			"pdc.tv",//

			"zakon.rada.gov.ua", //
			"zakon1.rada.gov.ua", //
			"media.mabila.ua",//

			"nufc.co.uk", //
			"cajt.pwp.blueyonder.co.uk" //
	));

	public static boolean contains(URI toTest) {
		return contains(HOSTS, toTest);
	}

}
