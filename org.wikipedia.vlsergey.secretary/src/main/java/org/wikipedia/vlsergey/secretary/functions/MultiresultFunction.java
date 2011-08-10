package org.wikipedia.vlsergey.secretary.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public abstract class MultiresultFunction<A, B> extends
		Function<Iterable<A>, Iterable<B>> {
	public MultiresultFunction<A, B> batchlazy(final int batchSize) {
		final MultiresultFunction<A, B> sourceFunction = this;
		return new MultiresultFunction<A, B>() {
			@Override
			public Iterable<B> apply(final Iterable<A> a) {
				return new Iterable<B>() {
					public Iterator<B> iterator() {
						return new Iterator<B>() {
							private Iterator<B> curentResult = Collections
									.<B> emptyList().iterator();

							private final Iterator<A> sourceIterator = a
									.iterator();

							public boolean hasNext() {
								return curentResult.hasNext()
										|| sourceIterator.hasNext();
							}

							public B next() {
								if (curentResult.hasNext())
									return curentResult.next();

								if (sourceIterator.hasNext()) {
									List<A> batch = new ArrayList<A>(batchSize);
									while (batch.size() < batchSize
											&& sourceIterator.hasNext()) {
										batch.add(sourceIterator.next());
									}
									this.curentResult = sourceFunction.apply(
											batch).iterator();
									return curentResult.next();
								}

								throw new NoSuchElementException();
							}

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
