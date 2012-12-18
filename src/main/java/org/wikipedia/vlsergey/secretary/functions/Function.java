package org.wikipedia.vlsergey.secretary.functions;

import java.util.Iterator;

public abstract class Function<A, R> {
	public abstract R apply(A a);

	public <B> Function<A, B> map(final Function<R, B> mapFunction) {
		return new Function<A, B>() {
			@Override
			public B apply(A a) {
				return mapFunction.apply(Function.this.apply(a));
			}
		};
	}

	public MultiresultFunction<A, R> toMultiresultFunction() {
		return new MultiresultFunction<A, R>() {
			@Override
			public Iterable<R> apply(final Iterable<A> a) {
				return new Iterable<R>() {
					@Override
					public Iterator<R> iterator() {
						final Iterator<A> source = a.iterator();
						return new Iterator<R>() {
							@Override
							public boolean hasNext() {
								return source.hasNext();
							}

							@Override
							public R next() {
								return Function.this.apply(source.next());
							}

							@Override
							public void remove() {
								source.remove();
							}
						};
					}
				};
			}
		};
	}
}
