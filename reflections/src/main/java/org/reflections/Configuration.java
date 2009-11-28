package org.reflections;

import com.google.common.base.Supplier;
import org.reflections.adapters.MetadataAdapter;
import org.reflections.serializers.Serializer;
import org.reflections.scanners.Scanner;

import java.net.URL;
import java.util.Set;
import java.util.concurrent.ExecutorService;

/**
 * Configuration is used to create a configured instance of {@link Reflections}
 * <p>it is preferred to use {@link org.reflections.util.ConfigurationBuilder}
 */
public interface Configuration {
    /** the scanner instances used for scanning different metadata */
    Set<Scanner> getScanners();

    /** the urls to be scanned */
    Set<URL> getUrls();

    /** the metadata adapter used to fetch metadata from classes */
    @SuppressWarnings({"RawUseOfParameterizedType"})
    MetadataAdapter getMetadataAdapter();

    /** the fully qualified name filter used to filter types to be scanned */
    boolean acceptsInput(String inputFqn);

    /** creates an executor service used to scan files */
    Supplier<ExecutorService> getExecutorServiceSupplier();

    /** the default serializer to use when saving Reflection */
    Serializer getSerializer();
}
