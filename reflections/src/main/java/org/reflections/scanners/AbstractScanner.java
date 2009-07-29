package org.reflections.scanners;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.reflections.Configuration;
import org.reflections.adapters.MetadataAdapter;

/**
 *
 */
@SuppressWarnings({"RawUseOfParameterizedType", "unchecked"})
public abstract class AbstractScanner implements Scanner {

	private Configuration configuration;
	protected Multimap<String, String> store;
	private Predicate<String> filter = Predicates.alwaysTrue();

	public Scanner filterBy(Predicate<String> filter) {
        this.filter = Predicates.or(this.filter, filter);
        return this;
    }

	public void setConfiguration(final Configuration configuration) {
		this.configuration = configuration;
	}

	public void setStore(final Multimap<String, String> store) {
		this.store = store;
	}

	protected MetadataAdapter getMetadataAdapter() {
		return configuration.getMetadataAdapter();
	}

    protected boolean accept(final String fqn) {
		return fqn != null && filter.apply(fqn);
	}
}
