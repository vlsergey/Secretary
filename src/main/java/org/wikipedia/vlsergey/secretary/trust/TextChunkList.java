package org.wikipedia.vlsergey.secretary.trust;

import gnu.trove.map.hash.TObjectLongHashMap;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.NullArgumentException;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class TextChunkList implements Comparable<TextChunkList> {

	public static final TextChunkList EMPTY = new TextChunkList(Collections.<TextChunk> emptyList());

	public static int calculateDifference(TextChunkList chunks1, TextChunkList chunks2) {
		TextChunkList[] suffixArray1 = chunks1.getSuffixArray();
		TextChunkList[] suffixArray2 = chunks2.getSuffixArray();

		int i = 0, j = 0;
		int difference = 0;

		while (i < suffixArray1.length && j < suffixArray2.length) {

			TextChunkList sublist1 = suffixArray1[i];
			TextChunkList sublist2 = suffixArray2[j];

			int compare = sublist1.get(0).compareTo(sublist2.get(0));

			if (compare < 0) {
				difference += sublist1.get(0).text.length();
				i++;
				continue;
			} else if (compare == 0) {
				i++;
				j++;
				continue;
			} else {
				difference += sublist2.get(0).text.length();
				j++;
				continue;
			}
		}

		for (; i < suffixArray1.length; i++) {
			difference += suffixArray1[i].get(0).text.length();
		}
		for (; j < suffixArray2.length; j++) {
			difference += suffixArray2[j].get(0).text.length();
		}

		return difference;
	}

	private static int compare(TextChunkList o1, TextChunkList o2, int commonLength) {
		final int o1Size = o1.size();
		final int o2Size = o2.size();
		final int maxSize = Math.max(o1Size, o2Size);

		for (int i = commonLength; i < maxSize + 1; i++) {
			if (o1Size == i) {
				if (o2Size == i) {
					return 0;
				} else {
					return -1;
				}
			} else if (o2Size == i) {
				return +1;
			}

			int current = o1.get(i).text.compareTo(o2.get(i).text);
			if (current != 0) {
				return current;
			}
		}

		return 0;
	}

	public static TextChunkList concatenate(Iterable<TextChunkList> lists) {
		List<TextChunk> result = new ArrayList<TextChunk>();
		for (TextChunkList list : lists) {
			result.addAll(list.textChunks);
		}
		return new TextChunkList(result);
	}

	public static String concatenate(TextChunk[] chunks, String delimeter) {
		StringBuilder result = new StringBuilder();
		for (TextChunk chunk : chunks) {
			result.append(chunk.text);
			result.append(delimeter);
		}
		return result.toString();
	}

	public static TextChunkList fromBinary(Locale locale, String text, byte[] bs) throws Exception {
		final ObjectInputStream objectInputStream = new ObjectInputStream(new GZIPInputStream(new ByteArrayInputStream(
				bs)));
		try {
			String[] authors = (String[]) objectInputStream.readObject();
			int[] authorship = (int[]) objectInputStream.readObject();

			return toTextChunkList(locale, authors, authorship, text);
		} finally {
			objectInputStream.close();
		}
	}

	public static int lcp(TextChunkList s, TextChunkList t) {
		int N = Math.min(s.size(), t.size());
		for (int i = 0; i < N; i++)
			if (!s.get(i).equals(t.get(i)))
				return i;
		return N;
	}

	public static TextChunkList lcs(TextChunkList list1, TextChunkList list2) {

		if (list1.size() == 0 || list2.size() == 0) {
			return EMPTY;
		}

		// al ready sorted
		TextChunkList[] suffixArray1 = list1.getSuffixArray();
		TextChunkList[] suffixArray2 = list2.getSuffixArray();

		{
			int index = Arrays.binarySearch(suffixArray1, list2);
			if (index >= 0) {
				return suffixArray1[index];
			}

			index = Arrays.binarySearch(suffixArray2, list1);
			if (index >= 0) {
				return list1;
			}

			// not found during binary search
			if (suffixArray1.length == 1 || suffixArray2.length == 1) {
				return EMPTY;
			}
		}

		int i = 0, j = 0;

		int candidateLength = 0;
		int candidateI = -1;

		while (i < suffixArray1.length && j < suffixArray2.length) {

			TextChunkList sublist1 = suffixArray1[i];
			TextChunkList sublist2 = suffixArray2[j];

			int commonLength = 0;
			if (sublist1.size() > candidateLength && sublist2.size() > candidateLength) {
				commonLength = lcp(sublist1, sublist2);
				if (commonLength > candidateLength) {
					candidateLength = commonLength;
					candidateI = i;
				}
			}

			int compare = compare(sublist1, sublist2, commonLength);

			if (compare < 0) {
				i++;
				continue;
			} else if (compare == 0) {
				i++;
				j++;
				continue;
			} else {
				j++;
				continue;
			}
		}

		if (candidateLength == 0) {
			return EMPTY;
		}

		return suffixArray1[candidateI].subList(0, candidateLength);
	}

	private static int length(Iterable<TextChunk> chunks) {
		int result = 0;
		for (TextChunk chunk : chunks) {
			result += chunk.text.length();
		}
		return result;
	}

	public static TextChunkList toTextChunkList(Locale locale, String user, String text) {
		text = text.toLowerCase(locale);
		text = text.replaceAll("<\\s?\\/?[a-zA-Z ]+>", "");
		text = text.replace('ё', 'е');
		text = StringUtils.join(StringUtils.split(text, "(){}[]<>«»:;,.?!\'\"\\/ \t\r\n—|=_~#$%^&*+~`"), ' ');

		List<TextChunk> chunks = new ArrayList<TextChunk>();
		for (String word : text.split(" ")) {
			if (!StopWords.RUSSIAN.contains(word)) {
				chunks.add(new TextChunk(user, word.intern()));
			}
		}
		return new TextChunkList(chunks);
	}

	private static TextChunkList toTextChunkList(Locale locale, String[] authors, int[] authorship, String text) {
		text = text.toLowerCase(locale);
		text = text.replaceAll("<\\s?\\/?[a-zA-Z ]+>", "");
		text = text.replace('ё', 'е');
		text = StringUtils.join(StringUtils.split(text, "(){}[]<>«»:;,.?!\'\"\\/ \t\r\n—|=_~#$%^&*+~`"), ' ');

		List<TextChunk> chunks = new ArrayList<TextChunk>();
		final String[] splitted = text.split(" ");

		int counter = 0;
		for (String word : splitted) {
			if (!StopWords.RUSSIAN.contains(word)) {
				chunks.add(new TextChunk(authors[authorship[counter]], word.intern()));
				counter++;
			}
		}

		if (counter != authorship.length) {
			throw new AssertionError();
		}

		return new TextChunkList(chunks);
	}

	private transient int hashCode = 0;

	private int length = -1;

	private String sortedHash = null;

	private transient TextChunkList[] suffixArray;

	private final List<TextChunk> textChunks;

	public TextChunkList(final List<TextChunk> textChunks) {
		if (textChunks == null) {
			throw new NullArgumentException("textChunks");
		}

		this.textChunks = textChunks;
	}

	@Override
	public int compareTo(TextChunkList o) {
		if (o.size() == 0) {
			if (this.size() == 0) {
				return 0;
			} else {
				return 1;
			}
		}

		int byFirst = get(0).text.compareTo(o.get(0).text);
		if (byFirst != 0) {
			return byFirst;
		}

		return compare(this, o, 1);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj || this.textChunks.equals(((TextChunkList) obj).textChunks);
	}

	public TextChunk get(int i) {
		return textChunks.get(i);
	}

	public LinkedHashMap<String, Double> getAuthorshipProcents() {
		final TObjectLongHashMap<String> byUsername = new TObjectLongHashMap<String>(16, 1, 0);

		long sum = 0;
		for (TextChunk textChunk : textChunks) {
			final int value = textChunk.text.length();
			byUsername.put(textChunk.user, byUsername.get(textChunk.user) + value);
			sum += value;
		}

		List<String> userNames = new ArrayList<String>(byUsername.keySet());
		Collections.sort(userNames, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				Long l1 = byUsername.get(o1);
				Long l2 = byUsername.get(o2);
				return l2.compareTo(l1);
			}
		});

		LinkedHashMap<String, Double> result = new LinkedHashMap<String, Double>();

		for (String userName : userNames) {
			long value = byUsername.get(userName);
			double procent = ((double) value) / sum;
			result.put(userName, Double.valueOf(procent));
		}

		return result;
	}

	public synchronized String getSortedHash() throws Exception {
		if (sortedHash == null) {
			TextChunk[] sorted = textChunks.toArray(new TextChunk[textChunks.size()]);
			Arrays.parallelSort(sorted);

			String concatenated = concatenate(sorted, " ");
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			this.sortedHash = Base64.encodeBase64String(md.digest(concatenated.getBytes("utf-8")));
		}
		return sortedHash;
	}

	public synchronized TextChunkList[] getSuffixArray() {
		if (suffixArray == null) {
			suffixArray = new TextChunkList[size()];
			for (int i = 0; i < size(); i++)
				suffixArray[i] = this.subList(i);
			Arrays.parallelSort(suffixArray, null);
		}
		return suffixArray;
	}

	@Override
	public int hashCode() {
		if (hashCode == 0) {
			hashCode = textChunks.hashCode();
		}
		return hashCode;
	}

	public int indexOf(TextChunk textChunk) {
		return textChunks.indexOf(textChunk);
	}

	public int indexOf(TextChunkList sublist) {

		if (this.size() < sublist.size()) {
			return -1;
		}

		if (this.size() == sublist.size()) {
			return this.equals(sublist) ? 0 : -1;
		}

		if (suffixArray != null) {
			// optimization if suffixes presents
			int index = Arrays.binarySearch(suffixArray, sublist);
			if (index < 0) {
				if (-index <= suffixArray.length) {
					if (lcp(sublist, suffixArray[-index - 1]) == sublist.size()) {
						return size() - suffixArray[-index - 1].size();
					} else {
						return -1;
					}
				} else {
					return -1;
				}
			} else if (index >= 0) {
				return size() - suffixArray[index].size();
			}
		}

		final int max = size() - sublist.size() + 1;
		final TextChunk first = sublist.get(0);

		for (int i = 0; i < max; i++) {
			if (get(i).equals(first)) {
				if (this.textChunks.subList(i, i + sublist.size()).equals(sublist.textChunks)) {
					return i;
				}
			}
		}
		return -1;
	}

	public boolean isEmpty() {
		return textChunks.isEmpty();
	}

	public synchronized int length() {
		if (length == -1) {
			length = length(textChunks);
		}
		return length;
	}

	public TextChunkList remove(TextChunkList sublist) {
		int index = this.indexOf(sublist);
		List<TextChunk> result = new ArrayList<TextChunk>();
		result.addAll(textChunks.subList(0, index));
		result.addAll(textChunks.subList(index + sublist.size(), textChunks.size()));
		return new TextChunkList(result);
	}

	public int size() {
		return textChunks.size();
	}

	public TextChunkList subList(int fromIndex) {
		return new TextChunkList(textChunks.subList(fromIndex, size()));
	}

	public TextChunkList subList(int fromIndex, int toIndex) {
		return new TextChunkList(textChunks.subList(fromIndex, toIndex));
	}

	byte[] toBinary() throws Exception {
		/*
		 * we assume that text of any revision is not changing, so it can be
		 * restored from binary using provided original text... if needed
		 */
		SortedSet<String> authorsSet = new TreeSet<String>();
		for (TextChunk textChunk : textChunks) {
			authorsSet.add(textChunk.user);
		}
		String[] authors = authorsSet.toArray(new String[authorsSet.size()]);

		int[] authorship = new int[textChunks.size()];
		for (int i = 0; i < textChunks.size(); i++) {
			TextChunk textChunk = textChunks.get(i);
			int authorIndex = Arrays.binarySearch(authors, textChunk.user);
			if (authorIndex < 0) {
				throw new AssertionError();
			}
			authorship[i] = authorIndex;
		}

		final ByteArrayOutputStream result = new ByteArrayOutputStream();
		ObjectOutputStream stream = new ObjectOutputStream(new GZIPOutputStream(result));
		try {
			stream.writeObject(authors);
			stream.writeObject(authorship);
		} finally {
			stream.close();
		}
		return result.toByteArray();
	}

	@Override
	public String toString() {
		return textChunks.toString();
	}

}
