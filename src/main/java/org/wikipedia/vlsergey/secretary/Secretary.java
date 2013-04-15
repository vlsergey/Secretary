package org.wikipedia.vlsergey.secretary;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wikipedia.vlsergey.secretary.books.CountBooks;
import org.wikipedia.vlsergey.secretary.books.ReplaceCiteBookWithSpecificTemplate;

public class Secretary {

	public static void main(String[] args) throws Exception {
		ApplicationContext appContext = new ClassPathXmlApplicationContext("application-context.xml");

		appContext.getBean(CountBooks.class).run();
		appContext.getBean(ReplaceCiteBookWithSpecificTemplate.class).run();

		while (true) {
			Thread.sleep(10000);
		}

	}
}
