package org.reflections;

import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.reflections.scanners.*;
import org.reflections.util.AbstractConfiguration;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.FilterBuilder;
import org.reflections.vfs.Vfs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import static java.util.Arrays.asList;
import java.util.List;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by IntelliJ IDEA.
 * User: ron
 * Date: Nov 7, 2009
 */
public class ReflectionsParallelTest extends ReflectionsTest {

    @BeforeClass
    public static void init() {
        Predicate<String> filter = new FilterBuilder().include("org.reflections.TestModel\\$.*");
        final int threads = Runtime.getRuntime().availableProcessors();

        reflections = new Reflections(new AbstractConfiguration()
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
