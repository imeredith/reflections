package org.reflections;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.reflections.serializers.Serializer;
import org.reflections.serializers.XmlSerializer;
import org.reflections.scanners.*;
import org.reflections.scanners.Scanner;
import org.reflections.util.*;
import static org.reflections.util.Utils.forNames;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import static java.lang.String.format;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;

/**
 * Reflections one-stop-shop object
 * <p>Reflections scans your classpath, indexes the metadata, allows you to query it on runtime
 * and may save and collect that information for many modules within your project.
 * <p>Using Reflections you can query your metadata such as:
 * <ul>
 * <li>get all subtypes of some type
 * <li>get all types/methods/fields annotated with some annotation, w/o annotation parameters matching
 * </ul>
 * <p>a typical use of Reflections would be:
 * <pre>
 * Reflections reflections = new Reflections("my.package.prefix"); //replace my.package.prefix with your package prefix, of course
 *
 * Set&#60Class&#60? extends SomeType>> subTypes = reflections.getSubTypesOf(SomeType.class);
 * Set&#60Class&#60?>> annotated = reflections.getTypesAnnotatedWith(SomeAnnotation.class);
 * Set&#60Class&#60?>> annotated1 = reflections.getTypesAnnotatedWith(
 *      new SomeAnnotation() {public String value() {return "1";}
 *                            public Class&#60? extends Annotation> annotationType() {return SomeAnnotation.class;}});
 * </pre>
 * basically, to use Reflections for scanning and querying, instantiate it with a {@link org.reflections.Configuration}, for example
 * <pre>
 *         new Reflections(
 *               new AbstractConfiguration() {
 *                   {
 *                      setFilter(new FilterBuilder().include("your project's common package prefix here..."));
 *                      setUrls(ClasspathHelper.getUrlsForCurrentClasspath());
 *                      setScanners(new SubTypesScanner(),
 *                                  new TypeAnnotationsScanner().filterBy(myClassAnnotationsFilter));
 *                      ));
 *                  }
 *         });
 * </pre>
 * and than use the convenient methods to query the metadata, such as {@link #getSubTypesOf},
 * {@link #getTypesAnnotatedWith}, {@link #getMethodsAnnotatedWith}, {@link #getFieldsAnnotatedWith}, {@link #getResources}
 * <br>use {@link #getStore()} to access and query the store directly
 * <p>in order to save a metadata use {@link #save(String)} or {@link #save(String, org.reflections.serializers.Serializer)}
 * for example with {@link org.reflections.serializers.XmlSerializer} or {@link org.reflections.serializers.JavaCodeSerializer}
 * <p>in order to collect pre saved metadata and avoid re-scanning, use {@link #collect(String, com.google.common.base.Predicate)}
 */
public class Reflections extends ReflectionUtils {
    private static final Logger log = LoggerFactory.getLogger(Reflections.class);

    final Configuration configuration;
    private final Store store;

    /**
     * constructs a Reflections instance and scan according to given {@link Configuration}
     * <p>it is prefered to use {@link org.reflections.util.AbstractConfiguration}
     */
    public Reflections(final Configuration configuration) {
        this.configuration = configuration;
        store = new Store();

        //inject to scanners
        for (Scanner scanner : configuration.getScanners()) {
            scanner.setConfiguration(configuration);
            scanner.setStore(store.get(scanner.getClass()));
        }

        scan();
    }

    /**
     * a convenient constructor for scanning within a package prefix
     * <p>if no scanners supplied, TypeAnnotationsScanner and SubTypesScanner are used by default
     */
    public Reflections(final String prefix, final Scanner... scanners) {
        this(new AbstractConfiguration() {
            final Predicate<String> filter = new FilterBuilder.Include(prefix);

            {
                Collection<URL> forPackagePrefix = ClasspathHelper.getUrlsForPackagePrefix(prefix);
                setUrls(forPackagePrefix);
                if (scanners == null || scanners.length == 0) {
                    setScanners(
                            new TypeAnnotationsScanner().filterResultsBy(filter),
                            new SubTypesScanner().filterResultsBy(filter));
                } else {
                    setScanners(scanners);
                }
            }
        });
    }

    protected Reflections() {
        configuration = null;
        store = new Store();
    }

    //
    protected void scan() {
        if (configuration.getUrls() == null || configuration.getUrls().isEmpty()) {
            log.error("given scan urls are empty. set urls in the configuration");
            return;
        }

        long time = System.currentTimeMillis();

        ExecutorService executorService = configuration.getExecutorServiceSupplier().get();
        List<Future<?>> futures = Lists.newArrayList();
        try {
            for (URL url : configuration.getUrls()) {
                Iterable<Vfs.File> files = Vfs.fromURL(url).getFiles();
                for (final Vfs.File file : files) {
                    Future<?> future = executorService.submit(new Runnable() {
                        public void run() {
                            String input = file.getRelativePath().replace('/', '.');
                            if (configuration.acceptsInput(input)) {
                                for (Scanner scanner : configuration.getScanners()) {
                                    if (scanner.acceptsInput(input)) {
                                        scanner.scan(file);
                                    }
                                }
                            }
                        }
                    });
                    futures.add(future);
                }
            }
        } finally {
            //todo use CompletionService
            for (Future<?> future : futures) {
                try {
                    future.get();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }
            executorService.shutdown();
        }

        time = System.currentTimeMillis() - time;

        Integer keys = store.getKeysCount();
        Integer values = store.getValuesCount();

        log.info(format("Reflections took %d ms to scan %d urls, producing %d keys and %d values %s",
                time, configuration.getUrls().size(), keys, values,
                executorService instanceof ThreadPoolExecutor ? format("[using %d cores]", ((ThreadPoolExecutor) executorService).getMaximumPoolSize()) : ""));
    }

    /** collect saved Reflection xml from all urls that contains the package META-INF/reflections and includes files matching the pattern .*-reflections.xml*/
    public static Reflections collect() {
        return collect("META-INF/reflections", new FilterBuilder().include(".*-reflections.xml"));
    }

    /**
     * collect saved Reflections xml from all urls that contains the given packagePrefix and matches the given resourceNameFilter
     * <p>
     * it is prefered to use a designated resource prefix (for example META-INF/reflections but not just META-INF),
     * so that relevant urls could be found much faster
     */
    public static Reflections collect(final String packagePrefix, final Predicate<String> resourceNameFilter) {
        final Reflections reflections = new Reflections();

        Iterable<Vfs.File> matchingFiles = Vfs.findFiles(ClasspathHelper.getUrlsForPackagePrefix(packagePrefix), packagePrefix, resourceNameFilter);

        for (final Vfs.File file : matchingFiles) {
            try {
                reflections.merge(new XmlSerializer().read(file.getInputStream()));
                log.info("Reflections collected metadata from " + file);
            } catch (IOException e) {
                throw new ReflectionsException("could not merge" + file, e);
            }
        }

        return reflections;
    }

    /**
     * merges a Reflections instance metadata into this instance
     */
    public Reflections merge(final Reflections reflections) {
        store.merge(reflections.store);
        return this;
    }

    //query

    /**
     * gets all sub types in hierarchy of a given type
     * <p/>depends on SubTypesScanner configured, otherwise an empty set is returned
     */
    public <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type) {
        Set<String> subTypes = store.getSubTypesOf(type.getName());
        return ImmutableSet.copyOf(Utils.<T>forNames(subTypes));
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>if given annotation is annotated with {@link java.lang.annotation.Inherited}, than inherited is honored.
     * otherwise @Inherited is not honored and all annotations are considerd inherited from interfaces and types to their subtypes
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other than a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured, otherwise an empty set is returned
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation) {
        Set<String> typesAnnotatedWith = store.getTypesAnnotatedWith(annotation.getName());
        return ImmutableSet.copyOf(forNames(typesAnnotatedWith));
    }

    //todo create a string version of these
    /**
     * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
     * <p><b>@Inherited is not honored</b>, all annotations are considerd inherited from interfaces and types to their subtypes
     * <p/>depends on TypeAnnotationsScanner configured, otherwise an empty set is returned
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation) {
        return getMatchingAnnotations(
                getTypesAnnotatedWith(annotation.annotationType()), annotation);
    }

    /**
     * get all methods annotated with a given annotation
     * <p/>depends on MethodAnnotationsScanner configured, otherwise an empty set is returned
     */
    public Set<Method> getMethodsAnnotatedWith(final Class<? extends Annotation> annotation) {
        Set<String> annotatedWith = store.getMethodsAnnotatedWith(annotation.getName());

        Set<Method> result = Sets.newHashSet();
        for (String annotated : annotatedWith) {
            result.add(DescriptorHelper.getMethodFromDescriptor(annotated));
        }

        return result;
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on MethodAnnotationsScanner configured, otherwise an empty set is returned
     */
    public Set<Method> getMethodsAnnotatedWith(final Annotation annotation) {
        return getMatchingAnnotations(
                getMethodsAnnotatedWith(annotation.annotationType()), annotation);
    }

    /**
     * get all fields annotated with a given annotation
     * <p/>depends on FieldAnnotationsScanner configured, otherwise an empty set is returned
     */
    public Set<Field> getFieldsAnnotatedWith(final Class<? extends Annotation> annotation) {
        final Set<Field> result = Sets.newHashSet();

        Collection<String> annotatedWith = store.getFieldsAnnotatedWith(annotation.getName());
        for (String annotated : annotatedWith) {
            result.add(DescriptorHelper.getFieldFromString(annotated));
        }

        return result;
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on FieldAnnotationsScanner configured, otherwise an empty set is returned
     */
    public Set<Field> getFieldsAnnotatedWith(final Annotation annotation) {
        return getMatchingAnnotations(
                getFieldsAnnotatedWith(annotation.annotationType()), annotation);
    }

    /**
     * get 'converter' methods that could effectively convert from type 'from' to type 'to'
     * <p>depends on ConvertersScanner configured, otherwise an empty set is returned
     *
     * @param from - the type to convert from
     * @param to   - the required return type
     */
    public Set<Method> getConverters(final Class<?> from, final Class<?> to) {
        Set<Method> result = Sets.newHashSet();

        Set<String> converters = store.getConverters(from.getName(), to.getName());
        for (String converter : converters) {
            result.add(DescriptorHelper.getMethodFromDescriptor(converter));
        }

        return result;
    }

    /** get resources relative paths where simple name (key) matches given namePredicate */
    public Set<String> getResources(final Predicate<String> namePredicate) {
        return store.getResources(namePredicate);
    }

    /** get resources relative paths where simple name (key) matches given regular expression
     * <pre>Set<String> xmls = reflections.getResources(".*\\.xml");</pre>*/
    public Set<String> getResources(final Pattern pattern) {
        return getResources(new Predicate<String>() {
            public boolean apply(String input) {
                return pattern.matcher(input).matches();
            }
        });
    }

    /** returns the store used for storing and querying the metadata */
    public Store getStore() {
        return store;
    }

    //
    /**
     * serialize to a given directory and filename
     * <p>* it is prefered to specify a designated directory (for example META-INF/reflections),
     * so that it could be found later much faster using the load method
     */
    public File save(final String filename) {
        return save(filename, configuration.getSerializer());
    }

    /**
     * serialize to a given directory and filename using given serializer
     * <p>* it is prefered to specify a designated directory (for example META-INF/reflections),
     * so that it could be found later much faster using the load method
     */
    public File save(final String filename, final Serializer serializer) {
        File file = serializer.save(this, filename);
        log.info("Reflections successfully saved in " + file + " using " + serializer.getClass().getSimpleName());
        return file;
    }
}
