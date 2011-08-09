package org.wikipedia.vlsergey.secretary;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.wikipedia.vlsergey.secretary.webcite.WebCiteTask;

public class Secretary {
	public static void main(String[] args) throws Exception {
		ApplicationContext appContext = new ClassPathXmlApplicationContext(
				"application-context.xml");

		WebCiteTask webCiteTask = appContext.getBean(WebCiteTask.class);
		webCiteTask.doTask("История криптографии");
	}
}
