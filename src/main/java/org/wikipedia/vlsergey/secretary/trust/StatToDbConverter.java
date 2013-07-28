package org.wikipedia.vlsergey.secretary.trust;

import gnu.trove.map.hash.TObjectLongHashMap;

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
import java.util.zip.GZIPInputStream;

import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class StatToDbConverter {

	public static void main(String[] args) throws Exception {
		new StatToDbConverter().collectToDb();
	}

	public void collectToDb() throws Exception {
		// clean all before start?

		final TObjectLongHashMap<String> counters = new TObjectLongHashMap<String>(16, 1, 0);
		for (File gzFile : new File("stats/pagecounts-raw/2013/06/").listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.endsWith(".gz") && name.contains("2013060");
			}
		})) {

			processFile(counters, gzFile);
			System.out.println("File " + gzFile + " processed");
		}

		List<String> articleName = new ArrayList<String>(counters.keySet());
		Collections.sort(articleName, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Long l1 = counters.get(o1);
				Long l2 = counters.get(o2);
				return l2.compareTo(l1);
			}
		});

		Writer fileWriter = new OutputStreamWriter(new FileOutputStream("stats/stats-2013-06-01.txt"), "utf-8");
		try {
			for (String key : articleName) {
				fileWriter.append(key + "\t" + counters.get(key) + "\n");
			}
		} finally {
			fileWriter.close();
		}
		System.out.println("Done!");

	}

	private void processFile(TObjectLongHashMap<String> counters, File gzFile) throws UnsupportedEncodingException,
			IOException, FileNotFoundException {
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(
				gzFile)), "utf-8"));
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

						if (pageName.contains(":") || pageName.startsWith("wiki/")) {
							continue;
						}
						if (!pageName.matches("[0-9a-zA-Zа-яА-ЯёЁ\\(\\)\\\\\\/\\-\\+\\?]+")) {
							continue;
						}
						long visits = Long.parseLong(strings[3]);
						counters.put(pageName, counters.get(pageName) + visits);

					} catch (IllegalArgumentException exc) {
						// skip
					}
				}
			}

		} finally {
			reader.close();
		}
	}
}
