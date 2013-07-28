package org.wikipedia.vlsergey.secretary.trust;

import java.util.Iterator;
import java.util.LinkedList;

public class PushBackIterator<E> implements Iterator<E> {

	private final LinkedList<E> backlog = new LinkedList<E>();

	private final Iterator<E> iterator;

	public PushBackIterator(Iterator<E> iterator) {
		this.iterator = iterator;
	}

	@Override
	public boolean hasNext() {
		return !backlog.isEmpty() || iterator.hasNext();
	}

	@Override
	public E next() {
		if (backlog.isEmpty()) {
			return iterator.next();
		} else {
			return backlog.removeLast();
		}
	}

	public E peekNext() {
		E next = next();
		pushBack(next);
		return next;
	}

	public void pushBack(E next) {
		backlog.add(next);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
