package org.reflections.adapters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Function;

/**
 * A ParallelStrategy which actually isn't parallel at all, just runs everything on the calling thread
 */
public class SingleThreadedParallelStrategy implements ParallelStrategy {

	public <K, V> List<V> transform(Iterator<K> source, Function<K, V> function) throws InterruptedException, ExecutionException {
		List<V> results = new ArrayList<V>();
		while (source.hasNext()) {
			K item = source.next();
			results.add(function.apply(item));
		}
		return results;
	}

	public int getParallelismLevel() {
		return 1;
	}

}
