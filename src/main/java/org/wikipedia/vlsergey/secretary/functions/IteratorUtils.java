package org.wikipedia.vlsergey.secretary.functions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IteratorUtils {

	public static <T> Stream<Collection<T>> batch(Iterable<T> iterable, final int size) {
		return batch(stream(iterable), size);
	}

	public static <T> Stream<Collection<T>> batch(Stream<T> stream, final int size) {

		final Iterator<T> iterator = stream.iterator();
		final Iterator<Collection<T>> result = new Iterator<Collection<T>>() {

			@Override
			public boolean hasNext() {
				return iterator.hasNext();
			}

			@Override
			public Collection<T> next() {
				List<T> result = new ArrayList<T>(size);
				for (int i = 0; i < size && iterator.hasNext(); i++) {
					result.add(iterator.next());
				}
				return result;
			}

		};

		return stream(result);
	}

	public static <T> Iterable<T> iterable(Stream<T> stream) {
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return stream.iterator();
			}
		};
	}

	public static <T> Stream<T> stream(Iterable<T> iterable) {
		return stream(iterable.iterator());
	}

	public static <T> Stream<T> stream(Iterator<T> iterator) {
		return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
	}
}
