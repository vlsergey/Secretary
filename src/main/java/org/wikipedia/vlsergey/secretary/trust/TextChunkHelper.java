package org.wikipedia.vlsergey.secretary.trust;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.wikipedia.vlsergey.secretary.jwpf.model.UserKey;
import org.wikipedia.vlsergey.secretary.trust.ProtobufHolder.Authorship;

import com.google.protobuf.ByteString;

public class TextChunkHelper {

	public static TextChunkList fromBinary(Locale locale, String text, byte[] bs) throws Exception {
		final String[] splitted = split(locale, text);

		Authorship authorship = Authorship.parseFrom(bs);
		List<Authorship.UserKey> aUserKeys = fromDictionaryAndIndexes(authorship.getUserKeysList(),
				authorship.getIndexesList());

		List<TextChunk> chunks = new ArrayList<TextChunk>();

		int counter = 0;
		for (String word : splitted) {
			if (!StopWords.RUSSIAN.contains(word)) {
				Authorship.UserKey aUserKey = aUserKeys.get(counter);
				UserKey authorKey = toUserKey(aUserKey);
				chunks.add(new TextChunk(authorKey, word.intern()));
				counter++;
			}
		}

		return new TextChunkList(chunks);
	}

	static <T> List<T> fromDictionaryAndIndexes(List<T> dict, List<Integer> indexes) {
		List<T> result = new ArrayList<T>(indexes.size());
		for (Integer index : indexes) {
			result.add(dict.get(index));
		}
		return result;
	}

	static String[] split(Locale locale, String text) {
		text = text.toLowerCase(locale);
		text = text.replaceAll("<\\s?\\/?[a-zA-Z ]+>", "");
		text = text.replace('ё', 'е');
		text = StringUtils.join(StringUtils.split(text, "(){}[]<>«»:;,.?!\'\"\\/ \t\r\n—|=_~#$%^&*+~`"), ' ');
		final String[] splitted = text.split(" ");
		return splitted;
	}

	static <T extends Comparable<T>> Pair<List<T>, List<Integer>> toDictionaryAndIndexes(List<T> src) {
		final SortedSet<T> sortedSet = new TreeSet<T>(src);
		List<T> sorted = new ArrayList<T>(sortedSet);
		List<Integer> indexes = new ArrayList<Integer>(src.size());

		for (T t : src) {
			int indexOf = Collections.binarySearch(sorted, t);
			if (indexOf < 0)
				throw new AssertionError();
			indexes.add(indexOf);
		}
		return new ImmutablePair<List<T>, List<Integer>>(sorted, indexes);
	}

	static Authorship.UserKey.Builder toProto(final UserKey userKey) {
		Authorship.UserKey.Builder aUserKeyBuilder = Authorship.UserKey.newBuilder();
		if (userKey.isAnonymous()) {
			aUserKeyBuilder.setInetAddress(ByteString.copyFrom(userKey.getInetAddress().getAddress()));
		} else {
			aUserKeyBuilder.setUserId(userKey.getUserId());
		}
		return aUserKeyBuilder;
	}

	static UserKey toUserKey(Authorship.UserKey aUserKey) throws UnknownHostException {
		UserKey authorKey;

		if (aUserKey.hasUserId()) {
			final long userId = aUserKey.getUserId();
			authorKey = new UserKey(userId);
		} else if (aUserKey.hasInetAddress()) {
			InetAddress inetAddress = InetAddress.getByAddress(aUserKey.getInetAddress().toByteArray());
			authorKey = new UserKey(inetAddress);
		} else {
			throw new IllegalArgumentException("Unable to extract author key from proto message");
		}
		return authorKey;
	}
}
