package org.wikipedia.vlsergey.secretary;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class Secretary {

	public static void main(String[] args) throws Exception {
		ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");

		while (true) {
			Thread.sleep(10000);
		}
	}
}
