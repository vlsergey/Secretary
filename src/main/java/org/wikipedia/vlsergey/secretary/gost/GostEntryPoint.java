package org.wikipedia.vlsergey.secretary.gost;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wikipedia.vlsergey.secretary.http.ExternalIpChecker;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;

public class GostEntryPoint {

	public static void main(String[] args) throws Exception {
		ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");

		ExternalIpChecker ExternalIpChecker = appContext.getBean(ExternalIpChecker.class);
		ExternalIpChecker.assertIpAddressesAreDifferent();

		MediaWikiBot mediaWikiBot = appContext.getBean(MediaWikiBot.class);
		mediaWikiBot.httpLogin();

		Gost gost = appContext.getBean(Gost.class);
		gost.run();
	}
}
