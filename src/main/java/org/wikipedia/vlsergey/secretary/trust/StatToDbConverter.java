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
		new StatToDbConverter().collectToDb("stats/pagecounts-raw/2013/07/", "201307", "stats/stats-2013-07.txt", 10);
		// new StatToDbConverter().collectToDb("stats/pagecounts-raw/2013/06/",
		// "201306", "stats/stats-2013-06.txt", 10);
	}

	private final ExecutorService executor = Executors.newFixedThreadPool(2);

	public void collectToDb(final String folder, final String fileNameContains, final String outputName, int minValue)
			throws Exception {

		final List<Future<?>> futures = new ArrayList<Future<?>>();
		final ConcurrentMap<String, AtomicLong> counters = new ConcurrentHashMap<String, AtomicLong>(10000);
		for (final File gzFile : new File(folder).listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".gz") && name.contains(fileNameContains);
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

		Writer fileWriter = new OutputStreamWriter(new FileOutputStream(outputName), "utf-8");
		try {
			for (String key : articleName) {
				final AtomicLong value = counters.get(key);
				if (value.longValue() < minValue) {
					break;
				}
				fileWriter.append(key + "\t" + value + "\n");
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

						if (pageName.codePointAt(0) == 65533) {
							// Windows-1251?
							pageName = URLDecoder.decode(strings[1], "Windows-1251");
							pageName = pageName.replace('_', ' ');
						}

						if (pageName.contains("\\x")) {
							pageName = pageName.replace("\\x", "%");
							pageName = URLDecoder.decode(pageName, "utf-8");
						}

						if (pageName.contains("\r") || pageName.contains("\n") || pageName.contains("\t")
								|| pageName.contains("\0")) {
							continue;
						}

						if (pageName.startsWith("Заглавная страница") || pageName.startsWith("Main page")
								|| pageName.equals("edit") || pageName.equals("null")) {
							continue;
						}

						{
							String lowerCase = pageName.toLowerCase();
							if (lowerCase.startsWith("w/")
									|| lowerCase.startsWith("wiki/")
									//
									|| lowerCase.startsWith("обсуждение:")
									//
									|| lowerCase.startsWith("категория:")
									|| lowerCase.startsWith("обсуждение категории:")
									//
									|| lowerCase.startsWith("арбитраж:")
									|| lowerCase.startsWith("обсуждение арбитража:")
									//
									|| lowerCase.startsWith("википедия:")
									|| lowerCase.startsWith("обсуждение википедии:")
									//
									|| lowerCase.startsWith("шаблон:")
									|| lowerCase.startsWith("обсуждение шаблона:")
									//
									|| lowerCase.startsWith("портал:")
									|| lowerCase.startsWith("обсуждение портала:")
									//
									|| lowerCase.startsWith("проект:")
									|| lowerCase.startsWith("обсуждение проекта:")
									//
									|| lowerCase.startsWith("user:") || lowerCase.startsWith("участник:")
									|| lowerCase.startsWith("участница:")
									|| lowerCase.startsWith("обсуждение участника:")
									|| lowerCase.startsWith("обсуждение участницы:")
									//
									|| lowerCase.startsWith("файл:") || lowerCase.startsWith("изображение:")
									|| lowerCase.startsWith("обсуждение файла:")
									//
									|| lowerCase.startsWith("special:") || lowerCase.startsWith("служебная:")) {
								continue;
							}
						}

						long visits = Long.parseLong(strings[2]);

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
