package org.reflections;

import com.google.common.base.Predicate;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.reflections.TestModel.*;
import org.reflections.util.FilterBuilder;
import org.reflections.scanners.*;
import org.reflections.util.AbstractConfiguration;
import org.reflections.util.ClasspathHelper;
import org.reflections.adapters.Jsr166ParallelStrategy;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import static java.util.Arrays.asList;
import java.util.Collection;
import java.util.Set;

/**
 *
 */
public class ReflectionsTest {
    private static Reflections reflections;

    @BeforeClass
    public static void init() {
        reflections = new Reflections(new TestConfiguration());
    }

    private static class TestConfiguration extends AbstractConfiguration {
        {
            setParallelStrategy(new Jsr166ParallelStrategy());
            Predicate<String> filter = new FilterBuilder().include(TestModel.class.getName());
            setScanners(
                    new SubTypesScanner().filterBy(filter),
                    new ClassAnnotationsScanner().filterBy(filter),
                    new FieldAnnotationsScanner().filterBy(filter),
                    new MethodAnnotationsScanner().filterBy(filter),
                    new ConvertersScanner().filterBy(filter));
            setUrls(asList(ClasspathHelper.getUrlForClass(TestModel.class)));
        }
    }

    @Test
    public void testAll() {
        testSubTypesOf();
        testTypesAnnotatedWith();
        testMethodsAnnotatedWith();
        testFieldsAnnotatedWith();
        testConverters();
    }

    @Test
    public void testSubTypesOf() {
        assertThat(reflections.getSubTypesOf(I1.class), are(I2.class, C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getSubTypesOf(I1.class, I2.class), are(I2.class, C1.class, C2.class, C3.class, C5.class));
    }

    @Test
    public void testTypesAnnotatedWith() {
        //@Inherited
        assertThat("@Inherited meta-annotation should not effect annotated annotations",
                reflections.getTypesAnnotatedWithInherited(MAI1.class), isEmpty);

        assertThat("@Inherited meta-annotation should not effect annotated interfaces",
                reflections.getTypesAnnotatedWithInherited(AI2.class), isEmpty);

        assertThat("@Inherited meta-annotation should only effect annotated superclasses and it's sub types",
                reflections.getTypesAnnotatedWithInherited(AC1.class), are(C1.class, C2.class, C3.class, C5.class));

        assertThat(reflections.getTypesAnnotatedWith(MAI1.class), are(AI1.class, I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));
        assertThat(reflections.getTypesAnnotatedWith(AI1.class), are(I1.class, I2.class, C1.class, C2.class, C3.class, C5.class));

        //annotation member value matching
        AC2 ac2 = new AC2() {
            public String value() {return "ugh?!";}
            public Class<? extends Annotation> annotationType() {return AC2.class;}};

        assertThat(reflections.getTypesAnnotatedWithInherited(ac2), isEmpty);

        assertThat("non @Inherited meta-annotation is effective on subtypes of interfaces and supertypes",
                reflections.getTypesAnnotatedWith(ac2), are(I3.class, C6.class, C3.class, C5.class));
    }

    @Test
    public void testMethodsAnnotatedWith() {
        try {
            assertThat(reflections.getMethodsAnnotatedWith(AM1.class),
                    are(C4.class.getMethod("m1"),
                        C4.class.getMethod("m2", int.class, String[].class),
                        C4.class.getMethod("m3")));

            assertThat(reflections.getMethodsAnnotatedWith(new AM1() {
                            public String value() {return "1";}
                            public Class<? extends Annotation> annotationType() {return AM1.class;}}),
                    are(C4.class.getMethod("m1"),
                        C4.class.getMethod("m2", int.class, String[].class)
                        ));
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test
    public void testFieldsAnnotatedWith() {
        try {
            assertThat(reflections.getFieldsAnnotatedWith(AF1.class),
                    are(C4.class.getDeclaredField("f1"),
                        C4.class.getDeclaredField("f2")
                        ));

            assertThat(reflections.getFieldsAnnotatedWith(new AF1() {
                            public String value() {return "2";}
                            public Class<? extends Annotation> annotationType() {return AF1.class;}}),
                    are(C4.class.getDeclaredField("f2")));
        } catch (NoSuchFieldException e) {
            fail();
        }
    }

    @Test
    public void testConverters() {
        try {
            assertThat(reflections.getConverters(C2.class, C3.class),
                    are(C4.class.getMethod("c2toC3", C2.class)));
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test
    public void collect() {
        Reflections testModelReflections = new Reflections("org.reflections");
        String baseDir = System.getProperty("user.dir") + "/target/test-classes";
        testModelReflections.save(baseDir + "/META-INF/reflections/testModel-reflections.xml");

        reflections = Reflections.collect();
        testAll();
    }

    //
    private final BaseMatcher<Set<Class<?>>> isEmpty = new BaseMatcher<Set<Class<?>>>() {
        public boolean matches(Object o) {
            return ((Collection<?>) o).isEmpty();
        }

        public void describeTo(Description description) {
            description.appendText("empty collection");
        }
    };

    private <T> Matcher<Set<? super T>> are(final T... ts) {
        final Collection<?> c1 = Arrays.asList(ts);
        return new BaseMatcher<Set<? super T>>() {
            public boolean matches(Object o) {
                Collection<?> c2 = (Collection<?>) o;
                return c1.containsAll(c2) && c2.containsAll(c1);
            }

            public void describeTo(Description description) {
                description.appendText("elements: ");
                description.appendValueList("(", ",", ")", ts);
            }
        };
    }
}
