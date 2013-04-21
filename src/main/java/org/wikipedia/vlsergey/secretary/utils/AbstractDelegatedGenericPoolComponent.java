package org.wikipedia.vlsergey.secretary.utils;

import org.apache.commons.pool.impl.GenericObjectPool;

public abstract class AbstractDelegatedGenericPoolComponent extends AbstractPoolComponent {

	@Override
	public void clear() {
		getObjectPool().clear();
	}

	@Override
	public void evict() throws Exception {
		getObjectPool().evict();
	}

	@Override
	public int getMaxActive() {
		return getObjectPool().getMaxActive();
	}

	@Override
	public int getMaxIdle() {
		return getObjectPool().getMaxIdle();
	}

	@Override
	public long getMaxWait() {
		return getObjectPool().getMaxWait();
	}

	@Override
	public long getMinEvictableIdleTimeMillis() {
		return getObjectPool().getMinEvictableIdleTimeMillis();
	}

	@Override
	public int getNumActive() {
		return getObjectPool().getNumActive();
	}

	@Override
	public int getNumIdle() {
		return getObjectPool().getNumIdle();
	}

	@Override
	public int getNumTestsPerEvictionRun() {
		return getObjectPool().getNumTestsPerEvictionRun();
	}

	protected abstract GenericObjectPool getObjectPool();

	@Override
	public boolean getTestOnBorrow() {
		return getObjectPool().getTestOnBorrow();
	}

	@Override
	public boolean getTestOnReturn() {
		return getObjectPool().getTestOnReturn();
	}

	@Override
	public boolean getTestWhileIdle() {
		return getObjectPool().getTestWhileIdle();
	}

	@Override
	public long getTimeBetweenEvictionRunsMillis() {
		return getObjectPool().getTimeBetweenEvictionRunsMillis();
	}

	@Override
	public byte getWhenExhaustedAction() {
		return getObjectPool().getWhenExhaustedAction();
	}

	@Override
	public void setMaxActive(int maxActive) {
		getObjectPool().setMaxActive(maxActive);
	}

	@Override
	public void setMaxIdle(int maxIdle) {
		getObjectPool().setMaxIdle(maxIdle);
	}

	@Override
	public void setMaxWait(long maxWait) {
		getObjectPool().setMaxWait(maxWait);
	}

	@Override
	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis) {
		getObjectPool().setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);
	}

	@Override
	public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun) {
		getObjectPool().setNumTestsPerEvictionRun(numTestsPerEvictionRun);
	}

	@Override
	public void setTestOnBorrow(boolean testOnBorrow) {
		getObjectPool().setTestOnBorrow(testOnBorrow);
	}

	@Override
	public void setTestOnReturn(boolean testOnReturn) {
		getObjectPool().setTestOnReturn(testOnReturn);
	}

	@Override
	public void setTestWhileIdle(boolean testWhileIdle) {
		getObjectPool().setTestWhileIdle(testWhileIdle);
	}

	@Override
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis) {
		getObjectPool().setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRunsMillis);
	}

	@Override
	public void setWhenExhaustedAction(byte whenExhaustedAction) {
		getObjectPool().setWhenExhaustedAction(whenExhaustedAction);
	}

}
