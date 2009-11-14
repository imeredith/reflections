package org.reflections.util;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import org.reflections.Configuration;
import org.reflections.adapters.*;
import org.reflections.serializers.Serializer;
import org.reflections.serializers.XmlSerializer;
import org.reflections.scanners.*;

import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * an abstract implementation of {@link org.reflections.Configuration}
 * <p>uses reasonalbe defaults, such as SingleThreadExecutor for scanning, accept all for {@link #inputsFilter}
 * , scanners set to {@link org.reflections.scanners.SubTypesScanner}, {@link org.reflections.scanners.TypeAnnotationsScanner},
 * {@link org.reflections.scanners.TypesScanner}, {@link org.reflections.scanners.TypeElementsScanner},
 */
@SuppressWarnings({"RawUseOfParameterizedType"})
public class AbstractConfiguration implements Configuration {
    private Set<Scanner> scanners;
    private Set<URL> urls;
    private MetadataAdapter metadataAdapter;
    private Predicate<String> inputsFilter;
    private Serializer serializer;
    private Supplier<ExecutorService> executorServiceSupplier;

    public AbstractConfiguration() {
        //defaults
        scanners = Sets.<Scanner>newHashSet(
                new SubTypesScanner(), new TypeAnnotationsScanner(), new TypesScanner(), new TypeElementsScanner());
        metadataAdapter = new JavassistAdapter();
        inputsFilter = Predicates.alwaysTrue();
        serializer = new XmlSerializer();
        executorServiceSupplier = new Supplier<ExecutorService>() {
            public ExecutorService get() {
                return Executors.newSingleThreadExecutor();
            }
        };
    }

    public Set<Scanner> getScanners() {
		return scanners;
	}

    /** set the scanners instances for scanning different metadata */
    public AbstractConfiguration setScanners(final Scanner... scanners) {
        this.scanners = ImmutableSet.of(scanners);
        return this;
    }

    public Set<URL> getUrls() {
        return urls;
    }

    /** set the urls to be scanned
     * <p>use {@link org.reflections.util.ClasspathHelper} convenient methods to get the relevant urls
     * */
    public AbstractConfiguration setUrls(final Collection<URL> urls) {
		this.urls = ImmutableSet.copyOf(urls);
        return this;
	}

    /** set the urls to be scanned
     * <p>use {@link org.reflections.util.ClasspathHelper} convenient methods to get the relevant urls
     * */
    public AbstractConfiguration setUrls(final URL... urls) {
		this.urls = ImmutableSet.of(urls);
        return this;
	}

    public MetadataAdapter getMetadataAdapter() {
        return metadataAdapter;
    }

    /** sets the metadata adapter used to fetch metadata from classes */
    public AbstractConfiguration setMetadataAdapter(final MetadataAdapter metadataAdapter) {
        this.metadataAdapter = metadataAdapter;
        return this;
    }

    public boolean acceptsInput(String inputFqn) {
        return inputsFilter.apply(inputFqn);
    }

    /** sets the input filter for all resources to be scanned
     * <p> supply a {@link com.google.common.base.Predicate} or use the {@link FilterBuilder}*/
    public AbstractConfiguration filterInputsBy(Predicate<String> inputsFilter) {
        this.inputsFilter = inputsFilter;
        return this;
    }

    public Supplier<ExecutorService> getExecutorServiceSupplier() {
        return executorServiceSupplier;
    }

    public AbstractConfiguration setExecutorServiceSupplier(Supplier<ExecutorService> executorServiceSupplier) {
        this.executorServiceSupplier = executorServiceSupplier;
        return this;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public AbstractConfiguration setSerializer(Serializer serializer) {
        this.serializer = serializer;
        return this;
    }
}
