package org.wikipedia.vlsergey.secretary.functions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

public abstract class MultiresultFunction<A, B> implements Function<Iterable<? extends A>, Iterable<B>> {

	public MultiresultFunction<A, B> makeBatched(final int batchSize) {
		final MultiresultFunction<A, B> sourceFunction = this;
		return new MultiresultFunction<A, B>() {
			@Override
			public Iterable<B> apply(final Iterable<? extends A> a) {
				return new Iterable<B>() {
					@Override
					public Iterator<B> iterator() {
						return new Iterator<B>() {
							private Iterator<B> curentResult = Collections.<B> emptyList().iterator();

							private final Iterator<? extends A> sourceIterator = a.iterator();

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
}
