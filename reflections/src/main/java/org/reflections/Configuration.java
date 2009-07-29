package org.reflections;

import com.google.common.base.Predicate;
import org.reflections.adapters.MetadataAdapter;
import org.reflections.scanners.Scanner;

import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Configuration is used to create a configured instance of {@link Reflections}
 * <p>it is prefered to use {@link org.reflections.util.AbstractConfiguration}
 */
@SuppressWarnings({"RawUseOfParameterizedType"})
public interface Configuration {
    /** the scanner instances used for scanning different metadata */
    Set<Scanner> getScanners();

    /** the urls to scan
     * <p>use {@link org.reflections.util.ClasspathHelper} convenient methods to get the relevant urls
     * */
    Set<URL> getUrls();

    /** */
    MetadataAdapter getMetadataAdapter();

    /** the fully qualified name filter used to filter types to be scanned */
    Predicate<String> getFilter();

    /** should or should not use fj (jsr166y) for parallely scanning types */
    boolean shouldUseForkjoin();
}
