package org.reflections;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reflections.scanners.*;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.FilterBuilder;

import static java.util.Arrays.asList;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** */
public class ReflectionsParallelTest extends ReflectionsTest {

    @BeforeClass
    public static void init() {
        Predicate<String> filter = new FilterBuilder().include("org.reflections.TestModel\\$.*");
        final int threads = Runtime.getRuntime().availableProcessors();

        reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(
                        new SubTypesScanner().filterResultsBy(filter),
                        new TypeAnnotationsScanner().filterResultsBy(filter),
                        new FieldAnnotationsScanner().filterResultsBy(filter),
                        new MethodAnnotationsScanner().filterResultsBy(filter),
                        new ConvertersScanner().filterResultsBy(filter))
                .setUrls(asList(ClasspathHelper.getUrlForClass(TestModel.class)))
                .setExecutorServiceSupplier(new Supplier<ExecutorService>() {
            public ExecutorService get() {
                return Executors.newFixedThreadPool(threads);
            }
        }));
    }

    @Test
    public void testAll() {
        super.testAll();
    }
}
