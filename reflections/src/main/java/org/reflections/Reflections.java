package org.reflections;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.reflections.scanners.*;
import org.reflections.serializers.Serializer;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.reflections.util.Utils;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static org.reflections.util.Utils.*;

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
 *      new Reflections(
 *          new ConfigurationBuilder()
 *              .filterInputsBy(new FilterBuilder().include("your project's common package prefix here..."))
 *              .setUrls(ClasspathHelper.forClassLoader())
 *              .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner()));
 * </pre>
 * and than use the convenient methods to query the metadata, such as {@link #getSubTypesOf},
 * {@link #getTypesAnnotatedWith}, {@link #getMethodsAnnotatedWith}, {@link #getFieldsAnnotatedWith}, {@link #getResources}
 * <br>use {@link #getStore()} to access and query the store directly
 * <p>in order to save a metadata use {@link #save(String)} or {@link #save(String, org.reflections.serializers.Serializer)}
 * for example with {@link org.reflections.serializers.XmlSerializer} or {@link org.reflections.serializers.JavaCodeSerializer}
 * <p>in order to collect pre saved metadata and avoid re-scanning, use {@link #collect(String, com.google.common.base.Predicate, org.reflections.serializers.Serializer...)}}
 * <p><i>* be aware that when using the constructor new Reflections("my.package"), only urls with prefix 'my.package' will be scanned,
 * and any transitive classes in other urls will not be scanned (for example if my.package.SomeClass extends other.package.OtherClass,
 * than the later will not be scanned). in that case use the other constructors and specify the relevant packages/urls
 * {@link Reflections#Reflections(Object[], org.reflections.scanners.Scanner...)}()} or {@link Reflections#Reflections(Configuration)}</i>
 * <p><p><p>For Javadoc, source code, and more information about Reflections Library, see http://code.google.com/p/reflections/
 */
public class Reflections extends ReflectionUtils {
    @Nullable public static Logger log;

    static {
        Logger logger; try { logger = LoggerFactory.getLogger(Reflections.class); } catch (Error e) { logger = null; }
        log = logger;
    }

    protected final transient Configuration configuration;
    private Store store;

    /**
     * constructs a Reflections instance and scan according to given {@link Configuration}
     * <p>it is preferred to use {@link org.reflections.util.ConfigurationBuilder}
     */
    public Reflections(final Configuration configuration) {
        this.configuration = configuration;
        store = new Store();

        //inject to scanners
        for (Scanner scanner : configuration.getScanners()) {
            scanner.setConfiguration(configuration);
            scanner.setStore(store.get(scanner));
        }

        scan();
    }

    /**
     * a convenient constructor for scanning within a package prefix.
     * <p>this actually create a {@link Configuration} with:
     * <br> - urls that contain resources with name {@code prefix}
     * <br> - filterInputsBy where name starts with the given {@code prefix}
     * <br> - scanners set to the given {@code scanners}, otherwise defaults to {@link TypeAnnotationsScanner} and {@link SubTypesScanner}.
     * @param prefix package prefix, to be used with {@link ClasspathHelper#forPackage(String, ClassLoader...)} )}
     * @param scanners optionally supply scanners, otherwise defaults to {@link TypeAnnotationsScanner} and {@link SubTypesScanner}
     */
    public Reflections(final String prefix, @Nullable final Scanner... scanners) {
        this(new String[]{prefix}, scanners);
    }

    /**
     * a convenient constructor for scanning within given package prefixes.
     * <br>for example
     * <pre>
     * Reflections reflections = new Reflections(new String[] {"my.package.prefix", "my.other.package.prefix"});
     * </pre>
     * <p>this actually create a {@link Configuration} with:
     * <br> - urls that contain resources with prefixes {@code prefixes}
     * <br> - filterInputsBy to include names starting with the given {@code prefixes}
     * <br> - scanners set to the given {@code scanners}, otherwise defaults to {@link TypeAnnotationsScanner} and {@link SubTypesScanner}.
     * @param prefixes string array of package prefixes, to be used with {@link ClasspathHelper#forPackage(String, ClassLoader...)} )}
     * @param scanners optionally supply scanners, otherwise defaults to {@link TypeAnnotationsScanner} and {@link SubTypesScanner}
     */
    public Reflections(final String[] prefixes, @Nullable final Scanner... scanners) {
        this(new ConfigurationBuilder() {
            {
                for (String prefix : prefixes) { addUrls(ClasspathHelper.forPackage(prefix)); }

                final FilterBuilder prefixFilter = new FilterBuilder();
                for (String prefix : prefixes) { prefixFilter.include(FilterBuilder.prefix(prefix)); }
                filterInputsBy(prefixFilter);

                setScanners(!isEmpty(scanners) ? scanners : new Scanner[]{new TypeAnnotationsScanner(), new SubTypesScanner()});
            }
        });
    }

    /**
     * a convenient constructor for scanning within given package prefixes and/or urls containing given classes.
     * <p>given urlHints is an array of either String or Class elements, where Strings results in scanning package prefix
     * and Class results in scanning url that contains that class, for example
     * <pre>
     *     new Reflections(new Object[] {"my.package", com.google.inject.Module.class, "javax.persistence"})
     * </pre>
     * would result in scanning packages 'my.package' and 'javax.persistence' and also the url that contains the class of com.google.inject.Module
     * <p>this actually create a {@link Configuration} with:
     * <br> - urls that contain resources with name {@code prefix} or that contains given classes
     * <br> - acceptsInput where name starts with the given {@code prefix} or with the classes package name
     * <br> - scanners set to the given {@code scanners}, otherwise defaults to {@link TypeAnnotationsScanner} and {@link SubTypesScanner}.
     * @param urlHints is an array of either String or Class elements, where Strings results in scanning package prefix and Class results in scanning urls containing the class
     * @param scanners optionally supply scanners, otherwise defaults to {@link TypeAnnotationsScanner} and {@link SubTypesScanner}
     */
    public Reflections(final Object[] urlHints, @Nullable final Scanner... scanners) {
        this(new ConfigurationBuilder() {
            {
                final FilterBuilder prefixFilter = new FilterBuilder();

                for (Object urlHint : urlHints) {
                    if (urlHint instanceof String) {
                        addUrls(ClasspathHelper.forPackage((String) urlHint));
                        prefixFilter.include(FilterBuilder.prefix((String) urlHint));
                    } else if (urlHint instanceof Class) {
                        addUrls(ClasspathHelper.forClass((Class) urlHint));
                        prefixFilter.includePackage(((Class) urlHint));
                    }
                }

                filterInputsBy(prefixFilter);

                setScanners(!isEmpty(scanners) ? scanners : new Scanner[]{new TypeAnnotationsScanner(), new SubTypesScanner()});
            }
        });
    }

    //used for serialization
    protected Reflections() {
        configuration = null;
    }

    //
    protected void scan() {
        if (configuration.getUrls() == null || configuration.getUrls().isEmpty()) {
            if (log != null) log.error("given scan urls are empty. set urls in the configuration");
            return;
        } else {
            if (log != null && log.isDebugEnabled()) {
                StringBuilder urls = new StringBuilder();
                for (URL url : configuration.getUrls()) {
                    urls.append("\t").append(url.toExternalForm()).append("\n");
                }
                log.debug("going to scan these urls:\n" + urls);
            }
        }

        long time = System.currentTimeMillis();

        ExecutorService executorService = configuration.getExecutorService();

        if (executorService == null) {
            for (URL url : configuration.getUrls()) {
                try {
                    for (final Vfs.File file : Vfs.fromURL(url).getFiles()) {
                        scan(file);
                    }
                } catch (ReflectionsException e) {
                    if (log != null) log.error("could not create Vfs.Dir from url. ignoring the exception and continuing", e);
                }

            }
        } else {
            //todo use CompletionService
            List<Future<?>> futures = Lists.newArrayList();
            try {
                for (URL url : configuration.getUrls()) {
                    try {
                        for (final Vfs.File file : Vfs.fromURL(url).getFiles()) {
                            futures.add(executorService.submit(new Runnable() {
                                public void run() {
                                    scan(file);
                                }
                            }));
                        }
                    } catch (ReflectionsException e) {
                        if (log != null) log.error("could not create Vfs.Dir from url. ignoring the exception and continuing", e);
                    }
                }

                for (Future future : futures) {
                    try { future.get(); } catch (Exception e) { throw new RuntimeException(e); }
                }
            } finally {
                executorService.shutdown();
            }
        }

        time = System.currentTimeMillis() - time;

        Integer keys = store.getKeysCount();
        Integer values = store.getValuesCount();

        if (log != null) log.info(format("Reflections took %d ms to scan %d urls, producing %d keys and %d values %s",
                time, configuration.getUrls().size(), keys, values,
                executorService != null && executorService instanceof ThreadPoolExecutor ?
                        format("[using %d cores]", ((ThreadPoolExecutor) executorService).getMaximumPoolSize()) : ""));
    }
    
    private void scan(Vfs.File file) {
        String input = file.getRelativePath().replace('/', '.');
        if (configuration.acceptsInput(input)) {
            for (Scanner scanner : configuration.getScanners()) {
                try {
                    if (scanner.acceptsInput(input)) {
                        scanner.scan(file);
                    }
                } catch (Exception e) {
                    log.warn("could not scan file " + file.getFullPath() + " with scanner " + scanner.getName(), e);
                }
            }
        }
    }

    /** collect saved Reflection xml resources and merge it into a Reflections instance
     * <p>by default, resources are collected from all urls that contains the package META-INF/reflections
     * and includes files matching the pattern .*-reflections.xml */
    public static Reflections collect() {
        return collect("META-INF/reflections", new FilterBuilder().include(".*-reflections.xml"));
    }

    /**
     * collect saved Reflections resources from all urls that contains the given packagePrefix and matches the given resourceNameFilter
     * and de-serializes them using the default serializer {@link org.reflections.serializers.XmlSerializer} or using the optionally supplied optionalSerializer
     * <p>
     * it is preferred to use a designated resource prefix (for example META-INF/reflections but not just META-INF),
     * so that relevant urls could be found much faster
     * @param optionalSerializer - optionally supply one serializer instance. if not specified or null, the default serializer will be used
     */
    public static Reflections collect(final String packagePrefix, final Predicate<String> resourceNameFilter, @Nullable Serializer... optionalSerializer) {
        ConfigurationBuilder configuration = new ConfigurationBuilder().setScanners();
        Serializer serializer = optionalSerializer != null && optionalSerializer.length == 1 ? optionalSerializer[0] : configuration.getSerializer();
        return new Reflections(configuration).collect(packagePrefix, resourceNameFilter, serializer);
    }

    /**
     * collect saved Reflections resources from all urls that contains the given packagePrefix and matches the given resourceNameFilter
     * and de-serializes them using the serializer configured in the configuration
     * <p>
     * it is preferred to use a designated resource prefix (for example META-INF/reflections but not just META-INF),
     * so that relevant urls could be found much faster
     */
    public Reflections collect(final String packagePrefix, final Predicate<String> resourceNameFilter, final Serializer serializer) {
        for (final Vfs.File file : Vfs.findFiles(ClasspathHelper.forPackage(packagePrefix), packagePrefix, resourceNameFilter)) {
            InputStream inputStream = null;
            try {
                inputStream = file.openInputStream();
                merge(serializer.read(inputStream));
                if (log != null) //noinspection ConstantConditions
                    log.info("Reflections collected metadata from " + file + " using serializer " + serializer.getClass().getName());
            } catch (IOException e) {
                throw new ReflectionsException("could not merge " + file, e);
            } finally {
                close(inputStream);
            }
        }

        return this;
    }

    /** merges saved Reflections resources from the given input stream, using the serializer configured in this instance's Configuration
     * <br> useful if you know the serialized resource location and prefer not to look it up the classpath
     * */
    public Reflections collect(final InputStream inputStream) {
        try {
            merge(configuration.getSerializer().read(inputStream));
            if (log != null) //noinspection ConstantConditions
                log.info("Reflections collected metadata from input stream using serializer " + configuration.getSerializer().getClass().getName());
        } catch (Exception ex) {
            throw new ReflectionsException("could not merge input stream", ex);
        }

        return this;
    }

    /** merges saved Reflections resources from the given file, using the serializer configured in this instance's Configuration
     *
     * useful if you know the serialized resource location and prefer not to look it up the classpath
     * */
    public Reflections collect(final File file) {
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(file);
            return collect(inputStream);
        } catch (FileNotFoundException e) {
            throw new ReflectionsException("could not obtain input stream from file " + file, e);
        } finally {
            Utils.close(inputStream);
        }
    }

    /**
     * merges a Reflections instance metadata into this instance
     */
    public Reflections merge(final Reflections reflections) {
        store.merge(reflections.store);
        return this;
    }

    //query

    @Nullable public <T extends Scanner> T get(Class<T> scannerClass) {
        for (Scanner scanner : configuration.getScanners()) {
            if (scanner.getClass().equals(scannerClass)) {
                //noinspection unchecked
                return (T) scanner;
            }
        }
        return null;
    }

    /**
     * gets all sub types in hierarchy of a given type
     * <p/>depends on SubTypesScanner configured, otherwise an empty set is returned
     */
    public <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type) {
        Set<String> subTypes = store.getSubTypesOf(type.getName());
        return ImmutableSet.copyOf(ReflectionUtils.<T>forNames(subTypes));
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is honored
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other than a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured, otherwise an empty set is returned
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation) {
        Set<String> typesAnnotatedWith = store.getTypesAnnotatedWith(annotation.getName());
        return ImmutableSet.copyOf(forNames(typesAnnotatedWith));
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is honored according to given honorInherited
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other than a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured, otherwise an empty set is returned
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation, boolean honorInherited) {
        Set<String> typesAnnotatedWith = store.getTypesAnnotatedWith(annotation.getName(), honorInherited);
        return ImmutableSet.copyOf(forNames(typesAnnotatedWith));
    }

    /**
     * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
     * <p>{@link java.lang.annotation.Inherited} is honored
     * <p/>depends on TypeAnnotationsScanner configured, otherwise an empty set is returned
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation) {
        return getTypesAnnotatedWith(annotation, true);
    }

    /**
     * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
     * <p>{@link java.lang.annotation.Inherited} is honored according to given honorInherited
     * <p/>depends on TypeAnnotationsScanner configured, otherwise an empty set is returned
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation, boolean honorInherited) {
        Set<String> types = store.getTypesAnnotatedWithDirectly(annotation.annotationType().getName());
        Set<Class<?>> annotated = getAll(forNames(types), withAnnotation(annotation));
        Set<String> inherited = store.getInheritedSubTypes(names(annotated), annotation.annotationType().getName(), honorInherited);
        return ImmutableSet.copyOf(forNames(inherited));
    }

    /**
     * get all methods annotated with a given annotation
     * <p/>depends on MethodAnnotationsScanner configured, otherwise an empty set is returned
     */
    public Set<Method> getMethodsAnnotatedWith(final Class<? extends Annotation> annotation) {
        Set<String> annotatedWith = store.getMethodsAnnotatedWith(annotation.getName());

        Set<Method> result = Sets.newHashSet();
        for (String annotated : annotatedWith) {
            result.add(getMethodFromDescriptor(annotated, configuration.getClassLoaders()));
        }

        return result;
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on MethodAnnotationsScanner configured, otherwise an empty set is returned
     */
    public Set<Method> getMethodsAnnotatedWith(final Annotation annotation) {
        return getAll(getMethodsAnnotatedWith(annotation.annotationType()), withAnnotation(annotation));
    }

    /**
     * get all fields annotated with a given annotation
     * <p/>depends on FieldAnnotationsScanner configured, otherwise an empty set is returned
     */
    public Set<Field> getFieldsAnnotatedWith(final Class<? extends Annotation> annotation) {
        final Set<Field> result = Sets.newHashSet();

        Collection<String> annotatedWith = store.getFieldsAnnotatedWith(annotation.getName());
        for (String annotated : annotatedWith) {
            result.add(getFieldFromString(annotated, configuration.getClassLoaders()));
        }

        return result;
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on FieldAnnotationsScanner configured, otherwise an empty set is returned
     */
    public Set<Field> getFieldsAnnotatedWith(final Annotation annotation) {
        return getAll(getFieldsAnnotatedWith(annotation.annotationType()), withAnnotation(annotation));
    }

    /** get resources relative paths where simple name (key) matches given namePredicate
     * <p>depends on ResourcesScanner configured, otherwise an empty set is returned
     * */
    public Set<String> getResources(final Predicate<String> namePredicate) {
        return store.getResources(namePredicate);
    }

    /** get resources relative paths where simple name (key) matches given regular expression
     * <p>depends on ResourcesScanner configured, otherwise an empty set is returned
     * <pre>Set<String> xmls = reflections.getResources(".*\\.xml");</pre>
     */
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
     * <p>* it is preferred to specify a designated directory (for example META-INF/reflections),
     * so that it could be found later much faster using the load method
     * <p>see the documentation for the save method on the configured {@link org.reflections.serializers.Serializer}
     */
    public File save(final String filename) {
        return save(filename, configuration.getSerializer());
    }

    /**
     * serialize to a given directory and filename using given serializer
     * <p>* it is preferred to specify a designated directory (for example META-INF/reflections),
     * so that it could be found later much faster using the load method
     */
    public File save(final String filename, final Serializer serializer) {
        File file = serializer.save(this, filename);
        if (log != null) //noinspection ConstantConditions
            log.info("Reflections successfully saved in " + file.getAbsolutePath() + " using " + serializer.getClass().getSimpleName());
        return file;
    }
}
