package org.wikipedia.vlsergey.secretary.trust;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;

import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class StatToDbConverter {

	public static void main(String[] args) throws Exception {
		new StatToDbConverter().collectToDb();
	}

	private final ExecutorService executor = Executors.newFixedThreadPool(2);

	public void collectToDb() throws Exception {
		// clean all before start?

		final List<Future<?>> futures = new ArrayList<Future<?>>();
		final ConcurrentMap<String, AtomicLong> counters = new ConcurrentHashMap<String, AtomicLong>(10000);
		for (final File gzFile : new File("stats/pagecounts-raw/2013/06/").listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".gz") && name.contains("201306");
			}
		})) {

			futures.add(executor.submit(new Runnable() {
				@Override
				public void run() {
					try {
						processFile(counters, gzFile);
						System.out.println("File " + gzFile + " processed");
					} catch (Exception exc) {
						exc.printStackTrace();
					}
				}
			}));
		}

		for (Future<?> future : futures) {
			future.get();
		}

		List<String> articleName = new ArrayList<String>(counters.keySet());
		sort(articleName, counters);

		final String outputName = "stats/stats-2013-06-01.txt";
		Writer fileWriter = new OutputStreamWriter(new FileOutputStream(outputName), "utf-8");
		try {
			for (String key : articleName) {
				fileWriter.append(key + "\t" + counters.get(key) + "\n");
			}
		} finally {
			fileWriter.close();
		}
		System.out.println("Done! -- " + outputName);
		executor.shutdown();
	}

	private void processFile(ConcurrentMap<String, AtomicLong> counters, File gzFile)
			throws UnsupportedEncodingException, IOException, FileNotFoundException {
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(
				gzFile)), "utf-8"), 1 << 20);
		try {
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.startsWith("ru ")) {
					String[] strings = StringUtils.split(line, " ");
					try {

						String pageName = URLDecoder.decode(strings[1], "utf-8");
						pageName = pageName.replace('_', ' ');

						if (pageName.contains("\\x")) {
							pageName = pageName.replace("\\x", "%");
							pageName = URLDecoder.decode(pageName, "utf-8");
						}

						if (pageName.startsWith("w/") || pageName.startsWith("wiki/")
								|| pageName.startsWith("Special:") || pageName.startsWith("Служебная:")
								|| pageName.startsWith("Участник:") || pageName.startsWith("Википедия:")
								|| pageName.startsWith("Файл:") || pageName.startsWith("Обсуждение:")
								|| pageName.startsWith("Обсуждение участника:")) {
							continue;
						}

						long visits = Long.parseLong(strings[3]);

						if (!counters.containsKey(pageName))
							counters.putIfAbsent(pageName, new AtomicLong());

						counters.get(pageName).addAndGet(visits);
					} catch (IllegalArgumentException exc) {
						// skip
					}
				}
			}

		} finally {
			reader.close();
		}
	}

	private void sort(List<String> articleName, final ConcurrentMap<String, AtomicLong> counters) {
		Collections.sort(articleName, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Long l1 = counters.get(o1).longValue();
				Long l2 = counters.get(o2).longValue();
				return l2.compareTo(l1);
			}
		});
	}
}
