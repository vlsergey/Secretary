package org.wikipedia.vlsergey.secretary.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public abstract class AbstractDaoLock {

	private static final int SEGMENTS = 128;

	private final Lock[] locks = new Lock[SEGMENTS];

	protected AbstractDaoLock() {
		for (int i = 0; i < SEGMENTS; i++) {
			locks[i] = new ReentrantLock();
		}
	}

	protected <T> T withLock(long id, Callable<T> action) {
		final int segmentIndex = (int) (id % SEGMENTS);
		final Lock lock = locks[segmentIndex];
		lock.lock();
		try {
			return action.call();
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			lock.unlock();
		}
	}

}
