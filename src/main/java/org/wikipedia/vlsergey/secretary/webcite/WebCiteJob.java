package org.wikipedia.vlsergey.secretary.webcite;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.webcite.lists.SkipReason;

public class WebCiteJob implements Runnable {

	private Locale locale;

	private MediaWikiBot mediaWikiBot;

	@Autowired
	private QueuedLinkProcessor queuedLinkProcessor;

	private QueuedPageProcessor queuedPageProcessor;

	public Locale getLocale() {
		return locale;
	}

	public MediaWikiBot getMediaWikiBot() {
		return mediaWikiBot;
	}

	// @Autowired
	// private WebCiteErrorCleanup webCiteErrorCleanup;

	public QueuedPageProcessor getQueuedPageProcessor() {
		return queuedPageProcessor;
	}

	@PostConstruct
	public void init() throws Exception {
		updateIgnoringList();
	}

	@Override
	public void run() {
		// webCiteErrorCleanup.errorCleanup("http://news.euro-coins.info/");
		// queuedLinkProcessor.clearQueue();
		queuedPageProcessor.run();
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public void setMediaWikiBot(MediaWikiBot mediaWikiBot) {
		this.mediaWikiBot = mediaWikiBot;
	}

	public void setQueuedPageProcessor(QueuedPageProcessor queuedPageProcessor) {
		this.queuedPageProcessor = queuedPageProcessor;
	}

	public void updateIgnoringList() throws Exception {
		for (SkipReason skipReason : SkipReason.values()) {
			updateIgnoringList(skipReason.collection(),
					"User:" + getMediaWikiBot().getLogin() + "/" + skipReason.botSubpageName());
		}
	}

	private void updateIgnoringList(Collection<String> hostsToIgnore, String pageName) throws Exception {
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
