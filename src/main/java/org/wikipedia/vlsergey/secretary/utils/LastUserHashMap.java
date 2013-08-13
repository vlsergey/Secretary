package org.wikipedia.vlsergey.secretary.utils;

import java.util.HashMap;
import java.util.Map;

public class LastUserHashMap<K, V> {

	static class Holder<V> {
		long timestamp = System.currentTimeMillis();

		final V value;

		public Holder(V value) {
			this.value = value;
		}
	}

	private int limit = 100;

	private final HashMap<K, Holder<V>> map = new HashMap<K, Holder<V>>();

	public LastUserHashMap(int limit) {
		this.limit = limit;
	}

	private void cleanup() {
		while (map.size() > limit) {
			K first = null;
			long min = Long.MAX_VALUE;
			for (Map.Entry<K, Holder<V>> entry : map.entrySet()) {
				if (entry.getValue().timestamp < min) {
					min = entry.getValue().timestamp;
					first = entry.getKey();
				}
			}
			map.remove(first);
		}
	}

	public boolean containsKey(K key) {
		Holder<V> value = map.get(key);
		if (value != null) {
			value.timestamp = System.currentTimeMillis();
			return true;
		}
		return false;
	}

	public V get(K key) {
		Holder<V> value = map.get(key);
		if (value != null) {
			value.timestamp = System.currentTimeMillis();
			return value.value;
		}
		return null;
	}

	public void put(K key, V value) {
		map.put(key, new Holder<V>(value));
		cleanup();
	}

}
