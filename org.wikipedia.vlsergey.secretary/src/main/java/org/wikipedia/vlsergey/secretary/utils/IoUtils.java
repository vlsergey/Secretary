package org.wikipedia.vlsergey.secretary.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;

public class IoUtils {

	public final static int BUFFER_LENGTH = 1 << 16; // 64 KB

	public static void copy(Reader in, Writer out) throws IOException {
		char[] buffer = new char[BUFFER_LENGTH];
		int readed = in.read(buffer, 0, BUFFER_LENGTH);
		while (readed != -1) {
			out.write(buffer, 0, readed);
			readed = in.read(buffer, 0, BUFFER_LENGTH);
		}
	}

	public static String readToString(InputStream inputStream, String enc)
			throws IOException {
		Reader in = new InputStreamReader(inputStream, enc);
		try {
			return readToString(in);
		} finally {
			in.close();
		}
	}

	public static String readToString(Reader source) throws IOException {
		StringWriter stringWriter = new StringWriter();
		copy(source, stringWriter);
		stringWriter.close();
		return stringWriter.toString();
	}

}
