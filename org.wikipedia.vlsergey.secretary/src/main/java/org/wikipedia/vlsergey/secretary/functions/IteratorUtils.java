package org.wikipedia.vlsergey.secretary.functions;

import java.util.Iterator;

public class IteratorUtils {
	public static <A, B> Iterable<B> map(final Iterable<A> source,
			final Function<A, B> mapFunction) {
		return new Iterable<B>() {
			@Override
			public Iterator<B> iterator() {
				final Iterator<A> sourceIterator = source.iterator();
				return new Iterator<B>() {
					@Override
					public boolean hasNext() {
						return sourceIterator.hasNext();
					}

					@Override
					public B next() {
						return mapFunction.apply(sourceIterator.next());
					}

					@Override
					public void remove() {
						sourceIterator.remove();
					}
				};
			}
		};
	}
}
