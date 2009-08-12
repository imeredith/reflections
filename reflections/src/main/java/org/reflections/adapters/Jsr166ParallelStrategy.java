package org.reflections.adapters;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import jsr166y.forkjoin.ForkJoinPool;
import jsr166y.forkjoin.Ops;
import jsr166y.forkjoin.ParallelArray;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

public final class Jsr166ParallelStrategy implements ParallelStrategy {
	private final ForkJoinPool forkJoinPool = new ForkJoinPool();

	public <K, V> List<V> transform(Iterator<K> source, final Function<K, V> function) throws InterruptedException, ExecutionException {
		ParallelArray<Object> parallelArray = ParallelArray.createFromCopy(Iterators.toArray(source, Object.class), forkJoinPool);
		return parallelArray.withMapping(new Ops.Mapper<Object, V>() {
			@SuppressWarnings("unchecked")
			public V map(Object key) {
				return function.apply((K) key);
			}
		}).all().asList();
	}

	public int getParallelismLevel() {
		return forkJoinPool.getParallelismLevel();
	}

}
