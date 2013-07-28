package org.wikipedia.vlsergey.secretary.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IoUtils {

	public final static int BUFFER_LENGTH = 1 << 16; // 64 KB

	private static final Logger logger = LoggerFactory.getLogger(IoUtils.class);

	private static byte[] compress(byte[] original) {
		try {
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			ZipOutputStream zipOutputStream = new ZipOutputStream(out);
			zipOutputStream.setLevel(4);
			zipOutputStream.putNextEntry(new ZipEntry("content"));
			zipOutputStream.write(original);
			zipOutputStream.close();
			byte[] compressed = out.toByteArray();
			logger.trace("Compressed " + original.length + " => " + compressed.length);
			return compressed;
		} catch (IOException exc) {
			throw new RuntimeException(exc);
		}
	}

	public static void copy(Reader in, Writer out) throws IOException {
		char[] buffer = new char[BUFFER_LENGTH];
		int readed = in.read(buffer, 0, BUFFER_LENGTH);
		while (readed != -1) {
			out.write(buffer, 0, readed);
			readed = in.read(buffer, 0, BUFFER_LENGTH);
		}
	}

	public static final byte[] decompress(byte[] content) {
		if (content == null || content.length == 0)
			return null;

		try {
			final ByteArrayInputStream in = new ByteArrayInputStream(content);
			ZipInputStream zipInputStream = new ZipInputStream(in);
			zipInputStream.getNextEntry();
			byte[] array = IOUtils.toByteArray(zipInputStream);
			return array;
		} catch (IOException exc) {
			throw new RuntimeException(exc);
		}
	}

	public static final String getHashcode(byte[] data) throws NoSuchAlgorithmException {
		MessageDigest messageDigest = MessageDigest.getInstance("MD5");
		messageDigest.update(data);
		byte[] result = messageDigest.digest();
		return Base64.encodeBase64String(result).trim();
	}

	public static String readToString(InputStream inputStream, String enc) throws IOException {
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

	public static final String stringFromBinary(byte[] content, boolean allowNull) {
		if (content == null && allowNull) {
			return null;
		}

		if (content == null || content.length == 0)
			return StringUtils.EMPTY;

		try {
			String string = new String(decompress(content), "utf-8");
			return string;
		} catch (IOException exc) {
			throw new RuntimeException(exc);
		}
	}

	public static final byte[] stringToBinary(String content, boolean allowNull) {
		if (content == null && allowNull) {
			return null;
		}
		if (StringUtils.isEmpty(content)) {
			return ArrayUtils.EMPTY_BYTE_ARRAY;
		}

		try {
			byte[] original = content.getBytes("utf-8");
			return compress(original);
		} catch (IOException exc) {
			throw new RuntimeException(exc);
		}
	}

}
