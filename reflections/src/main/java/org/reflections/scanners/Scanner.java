package org.reflections.scanners;

import com.google.common.base.Predicate;
import com.google.common.collect.Multimap;
import org.reflections.Configuration;
import org.reflections.vfs.Vfs;

import java.util.Set;

/**
 *
 */
public interface Scanner {

    String getName();

    boolean acceptsInput(String file);

    void scan(Vfs.File file);

    Scanner filterResultsBy(Predicate<String> filter);

	void setConfiguration(Configuration configuration);

	void setStore(Multimap<String, String> store);
}
