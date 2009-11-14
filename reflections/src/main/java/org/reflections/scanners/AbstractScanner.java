package org.reflections.scanners;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import org.reflections.Configuration;
import org.reflections.ReflectionsException;
import org.reflections.adapters.MetadataAdapter;
import org.reflections.vfs.Vfs;

import java.util.Set;
import java.io.InputStream;
import java.io.IOException;

/**
 *
 */
@SuppressWarnings({"RawUseOfParameterizedType", "unchecked"})
public abstract class AbstractScanner implements Scanner {

	private Configuration configuration;
	private Multimap<String, String> store;
	private Predicate<String> resultFilter = Predicates.alwaysTrue(); //accept all by default

    public String getName() {
        return getClass().getName();
    }

    public boolean acceptsInput(String file) {
        return file.endsWith(".class"); //is a class file
    }

    public void scan(Vfs.File file) {
        InputStream inputStream = null;
        try {
            inputStream = file.getInputStream();
            Object cls = configuration.getMetadataAdapter().createClassObject(inputStream);
            scan(cls);
        } catch (IOException e) {
            throw new RuntimeException("could not create class file from " + file.getName(), e);
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                throw new ReflectionsException("could not close input stream", e);
            }
        }
    }

    public abstract void scan(Object cls);

    //
    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    public Multimap<String, String> getStore() {
        return store;
    }

    public void setStore(final Multimap<String, String> store) {
        this.store = store;
    }

    public Predicate<String> getResultFilter() {
        return resultFilter;
    }

    public void setResultFilter(Predicate<String> resultFilter) {
        this.resultFilter = resultFilter;
    }

    public Scanner filterResultsBy(Predicate<String> filter) {
        this.setResultFilter(filter); return this;
    }

    //
    protected boolean acceptResult(final String fqn) {
		return fqn != null && getResultFilter().apply(fqn);
	}

	protected MetadataAdapter getMetadataAdapter() {
		return configuration.getMetadataAdapter();
	}
}
