/*
 * This class is distributed as part of the Psi Mod.
 * Get the Source Code in github:
 * https://github.com/Vazkii/Psi
 *
 * Psi is Open Source and distributed under the
 * Psi License: https://psi.vazkii.net/license.php
 */
package vazkii.psi.common.util;

import java.lang.ref.WeakReference;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 内存优化工具类 - 第五阶段性能调优
 * 提供对象池、缓存管理和内存使用优化功能
 */
public class MemoryOptimizer {

	// 对象池管理
	private static final Map<Class<?>, Queue<Object>> OBJECT_POOLS = new ConcurrentHashMap<>();
	private static final int MAX_POOL_SIZE = 100;

	// 弱引用缓存
	private static final Map<String, WeakReference<Object>> WEAK_CACHE = new ConcurrentHashMap<>();

	/**
	 * 获取预分配容量的ArrayList
	 */
	public static <T> ArrayList<T> createOptimizedList(int expectedSize) {
		return new ArrayList<>(Math.max(16, expectedSize));
	}

	/**
	 * 获取预分配容量的HashMap
	 */
	public static <K, V> HashMap<K, V> createOptimizedMap(int expectedSize) {
		return new HashMap<>(Math.max(16, (int) (expectedSize / 0.75f) + 1));
	}

	/**
	 * 获取预分配容量的HashSet
	 */
	public static <T> HashSet<T> createOptimizedSet(int expectedSize) {
		return new HashSet<>(Math.max(16, (int) (expectedSize / 0.75f) + 1));
	}

	/**
	 * 对象池获取
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getFromPool(Class<T> clazz) {
		Queue<Object> pool = OBJECT_POOLS.get(clazz);
		if(pool != null && !pool.isEmpty()) {
			return (T) pool.poll();
		}
		return null;
	}

	/**
	 * 对象池归还
	 */
	public static <T> void returnToPool(Class<T> clazz, T object) {
		if(object == null)
			return;

		Queue<Object> pool = OBJECT_POOLS.computeIfAbsent(clazz, k -> new ArrayDeque<>());
		if(pool.size() < MAX_POOL_SIZE) {
			pool.offer(object);
		}
	}

	/**
	 * 弱引用缓存存储
	 */
	public static void putWeakCache(String key, Object value) {
		WEAK_CACHE.put(key, new WeakReference<>(value));
	}

	/**
	 * 弱引用缓存获取
	 */
	@SuppressWarnings("unchecked")
	public static <T> T getWeakCache(String key) {
		WeakReference<Object> ref = WEAK_CACHE.get(key);
		if(ref != null) {
			Object value = ref.get();
			if(value == null) {
				WEAK_CACHE.remove(key);
			}
			return (T) value;
		}
		return null;
	}

	/**
	 * 清理过期的弱引用
	 */
	public static void cleanupWeakCache() {
		WEAK_CACHE.entrySet().removeIf(entry -> entry.getValue().get() == null);
	}

	/**
	 * 获取内存使用统计
	 */
	public static MemoryStats getMemoryStats() {
		Runtime runtime = Runtime.getRuntime();
		long totalMemory = runtime.totalMemory();
		long freeMemory = runtime.freeMemory();
		long usedMemory = totalMemory - freeMemory;
		long maxMemory = runtime.maxMemory();

		return new MemoryStats(totalMemory, freeMemory, usedMemory, maxMemory,
				OBJECT_POOLS.size(), WEAK_CACHE.size());
	}

	/**
	 * 内存统计数据类
	 */
	public static class MemoryStats {
		public final long totalMemory;
		public final long freeMemory;
		public final long usedMemory;
		public final long maxMemory;
		public final int poolCount;
		public final int cacheSize;

		public MemoryStats(long totalMemory, long freeMemory, long usedMemory,
				long maxMemory, int poolCount, int cacheSize) {
			this.totalMemory = totalMemory;
			this.freeMemory = freeMemory;
			this.usedMemory = usedMemory;
			this.maxMemory = maxMemory;
			this.poolCount = poolCount;
			this.cacheSize = cacheSize;
		}

		public double getMemoryUsagePercent() {
			return (double) usedMemory / maxMemory * 100;
		}

		@Override
		public String toString() {
			return String.format("Memory: %.1f%% used (%d/%d MB), Pools: %d, Cache: %d",
					getMemoryUsagePercent(),
					usedMemory / 1024 / 1024,
					maxMemory / 1024 / 1024,
					poolCount,
					cacheSize);
		}
	}
}
