package org.wikipedia.vlsergey.secretary.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class MultiresultFunction<A, B> extends Function<Iterable<A>, Iterable<B>> {

	public MultiresultFunction<A, B> makeBatched(final int batchSize) {
		final MultiresultFunction<A, B> sourceFunction = this;
		return new MultiresultFunction<A, B>() {
			@Override
			public Iterable<B> apply(final Iterable<A> a) {
				return new Iterable<B>() {
					@Override
					public Iterator<B> iterator() {
						return new Iterator<B>() {
							private Iterator<B> curentResult = Collections.<B> emptyList().iterator();

							private final Iterator<A> sourceIterator = a.iterator();

							@Override
							public boolean hasNext() {
								while (!curentResult.hasNext() && sourceIterator.hasNext()) {
									List<A> batch = new ArrayList<A>(batchSize);
									while (batch.size() < batchSize && sourceIterator.hasNext()) {
										batch.add(sourceIterator.next());
									}
									this.curentResult = sourceFunction.apply(batch).iterator();
									// repeat until non-empty result
								}
								return curentResult.hasNext();
							}

							@Override
							public B next() {
								if (!hasNext()) {
									throw new NoSuchElementException();
								}
								return curentResult.next();
							}

							@Override
							public void remove() {
								throw new UnsupportedOperationException();
							}

						};
					}
				};
			}
		};
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public MultiresultFunction<A, B> sortResults() {
		final MultiresultFunction<A, B> sourceFunction = this;
		return new MultiresultFunction<A, B>() {
			@Override
			public Iterable<B> apply(Iterable<A> a) {
				List<B> results = new ArrayList<B>();
				for (B b : sourceFunction.apply(a)) {
					results.add(b);
				}
				Collections.sort((List<Comparable>) results);
				return results;
			}
		};
	}
}
