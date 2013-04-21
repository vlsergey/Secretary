package org.wikipedia.vlsergey.secretary.utils;

import org.apache.commons.pool.PoolableObjectFactory;

public interface AbstractPoolComponentMBean {

	/**
	 * Clears
	 * 
	 * @throws Exception
	 */
	public void clear() throws Exception;

	/**
	 * Evicts
	 * 
	 * @throws Exception
	 */
	public void evict() throws Exception;

	/**
	 * Returns the cap on the total number of active instances from my pool.
	 * 
	 * @return the cap on the total number of active instances from my pool.
	 * @see #setMaxActive
	 */
	public int getMaxActive();

	/**
	 * Returns the cap on the number of "idle" instances in the pool.
	 * 
	 * @return the cap on the number of "idle" instances in the pool.
	 * @see #setMaxIdle
	 */
	public int getMaxIdle();

	/**
	 * Returns the maximum amount of time (in milliseconds) the
	 * {@link #borrowObject} method should block before throwing an exception
	 * when the pool is exhausted and the {@link #setWhenExhaustedAction
	 * "when exhausted" action} is {@link #WHEN_EXHAUSTED_BLOCK}.
	 * 
	 * When less than 0, the {@link #borrowObject} method may block
	 * indefinitely.
	 * 
	 * @see #setMaxWait
	 * @see #setWhenExhaustedAction
	 * @see #WHEN_EXHAUSTED_BLOCK
	 * @return the maximum amount of time (in milliseconds)
	 */
	public long getMaxWait();

	/**
	 * Returns the minimum amount of time an object may sit idle in the pool
	 * before it is eligable for eviction by the idle object evictor (if any).
	 * 
	 * @see #setMinEvictableIdleTimeMillis
	 * @see #setTimeBetweenEvictionRunsMillis
	 * @return the minimum amount of time an object may sit idle in the pool
	 *         before it is eligable for eviction by the idle object evictor (if
	 *         any).
	 */
	public long getMinEvictableIdleTimeMillis();

	/**
	 * @return numder of active
	 */
	public int getNumActive();

	/**
	 * @return number of idle
	 */
	public int getNumIdle();

	/**
	 * Returns the number of objects to examine during each run of the idle
	 * object evictor thread (if any).
	 * 
	 * @see #setNumTestsPerEvictionRun
	 * @see #setTimeBetweenEvictionRunsMillis
	 * @return the number of objects to examine during each run of the idle
	 *         object evictor thread (if any).
	 */
	public int getNumTestsPerEvictionRun();

	/**
	 * When <tt>true</tt>, objects will be
	 * {@link PoolableObjectFactory#validateObject validated} before being
	 * returned by the {@link #borrowObject} method. If the object fails to
	 * validate, it will be dropped from the pool, and we will attempt to borrow
	 * another.
	 * 
	 * @see #setTestOnBorrow
	 * @return test result
	 * 
	 */
	public boolean getTestOnBorrow();

	/**
	 * When <tt>true</tt>, objects will be
	 * {@link PoolableObjectFactory#validateObject validated} before being
	 * returned to the pool within the {@link #returnObject}.
	 * 
	 * @see #setTestOnReturn
	 * @return test result
	 */
	public boolean getTestOnReturn();

	/**
	 * When <tt>true</tt>, objects will be
	 * {@link PoolableObjectFactory#validateObject validated} by the idle object
	 * evictor (if any). If an object fails to validate, it will be dropped from
	 * the pool.
	 * 
	 * @see #setTestWhileIdle
	 * @see #setTimeBetweenEvictionRunsMillis
	 * @return test result
	 */
	public boolean getTestWhileIdle();

	/**
	 * Returns the number of milliseconds to sleep between runs of the idle
	 * object evictor thread. When non-positive, no idle object evictor thread
	 * will be run.
	 * 
	 * @see #setTimeBetweenEvictionRunsMillis
	 * @return he number of milliseconds to sleep between runs of the idle
	 *         object evictor thread. When non-positive, no idle object evictor
	 *         thread will be run.
	 * 
	 */
	public long getTimeBetweenEvictionRunsMillis();

	/**
	 * Returns the action to take when the {@link #borrowObject} method is
	 * invoked when the pool is exhausted (the maximum number of "active"
	 * objects has been reached).
	 * 
	 * @return one of {@link #WHEN_EXHAUSTED_BLOCK},
	 *         {@link #WHEN_EXHAUSTED_FAIL} or {@link #WHEN_EXHAUSTED_GROW}
	 * @see #setWhenExhaustedAction
	 */
	public byte getWhenExhaustedAction();

	/**
	 * Sets the cap on the total number of active instances from my pool.
	 * 
	 * @param maxActive
	 *            The cap on the total number of active instances from my pool.
	 *            Use a negative value for an infinite number of instances.
	 * @see #getMaxActive
	 */
	public void setMaxActive(int maxActive);

	/**
	 * Sets the cap on the number of "idle" instances in the pool.
	 * 
	 * @param maxIdle
	 *            The cap on the number of "idle" instances in the pool. Use a
	 *            negative value to indicate an unlimited number of idle
	 *            instances.
	 * @see #getMaxIdle
	 */
	public void setMaxIdle(int maxIdle);

	/**
	 * Sets the maximum amount of time (in milliseconds) the
	 * {@link #borrowObject} method should block before throwing an exception
	 * when the pool is exhausted and the {@link #setWhenExhaustedAction
	 * "when exhausted" action} is {@link #WHEN_EXHAUSTED_BLOCK}.
	 * 
	 * When less than 0, the {@link #borrowObject} method may block
	 * indefinitely.
	 * 
	 * @see #getMaxWait
	 * @see #setWhenExhaustedAction
	 * @see #WHEN_EXHAUSTED_BLOCK
	 * @param maxWait
	 */
	public void setMaxWait(long maxWait);

	/**
	 * Sets the minimum amount of time an object may sit idle in the pool before
	 * it is eligable for eviction by the idle object evictor (if any). When
	 * non-positive, no objects will be evicted from the pool due to idle time
	 * alone.
	 * 
	 * @see #getMinEvictableIdleTimeMillis
	 * @see #setTimeBetweenEvictionRunsMillis
	 * @param minEvictableIdleTimeMillis
	 */
	public void setMinEvictableIdleTimeMillis(long minEvictableIdleTimeMillis);

	/**
	 * Sets the number of objects to examine during each run of the idle object
	 * evictor thread (if any).
	 * <p>
	 * When a negative value is supplied,
	 * <tt>ceil({@link #getNumIdle})/abs({@link #getNumTestsPerEvictionRun})</tt>
	 * tests will be run. I.e., when the value is <i>-n</i>, roughly one
	 * <i>n</i>th of the idle objects will be tested per run.
	 * 
	 * @see #getNumTestsPerEvictionRun
	 * @see #setTimeBetweenEvictionRunsMillis
	 * @param numTestsPerEvictionRun
	 */
	public void setNumTestsPerEvictionRun(int numTestsPerEvictionRun);

	/**
	 * When <tt>true</tt>, objects will be
	 * {@link PoolableObjectFactory#validateObject validated} before being
	 * returned by the {@link #borrowObject} method. If the object fails to
	 * validate, it will be dropped from the pool, and we will attempt to borrow
	 * another.
	 * 
	 * @see #getTestOnBorrow
	 * @param testOnBorrow
	 */
	public void setTestOnBorrow(boolean testOnBorrow);

	/**
	 * When <tt>true</tt>, objects will be
	 * {@link PoolableObjectFactory#validateObject validated} before being
	 * returned to the pool within the {@link #returnObject}.
	 * 
	 * @see #getTestOnReturn
	 * @param testOnReturn
	 */
	public void setTestOnReturn(boolean testOnReturn);

	/**
	 * When <tt>true</tt>, objects will be
	 * {@link PoolableObjectFactory#validateObject validated} by the idle object
	 * evictor (if any). If an object fails to validate, it will be dropped from
	 * the pool.
	 * 
	 * @see #getTestWhileIdle
	 * @see #setTimeBetweenEvictionRunsMillis
	 * @param testWhileIdle
	 */
	public void setTestWhileIdle(boolean testWhileIdle);

	/**
	 * Sets the number of milliseconds to sleep between runs of the idle object
	 * evictor thread. When non-positive, no idle object evictor thread will be
	 * run.
	 * 
	 * @see #getTimeBetweenEvictionRunsMillis
	 * @param timeBetweenEvictionRunsMillis
	 */
	public void setTimeBetweenEvictionRunsMillis(long timeBetweenEvictionRunsMillis);

	/**
	 * Sets the action to take when the {@link #borrowObject} method is invoked
	 * when the pool is exhausted (the maximum number of "active" objects has
	 * been reached).
	 * 
	 * @param whenExhaustedAction
	 *            the action code, which must be one of
	 *            {@link #WHEN_EXHAUSTED_BLOCK}, {@link #WHEN_EXHAUSTED_FAIL},
	 *            or {@link #WHEN_EXHAUSTED_GROW}
	 * @see #getWhenExhaustedAction
	 */
	public void setWhenExhaustedAction(byte whenExhaustedAction);

}
