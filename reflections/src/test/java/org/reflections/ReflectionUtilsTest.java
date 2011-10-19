package org.reflections;

import com.google.common.base.Predicates;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Set;

import static org.reflections.Reflections.*;

/**
 * @author mamo
 */
public class ReflectionUtilsTest {

    @Test public void t1() {
        Set<Field> f1 = getAllFields(TestModel.C4.class, withName("f1"));
        Set<Field> f2 = getAllFields(TestModel.C4.class, withAnnotation(TestModel.AF1.class));
        Set<Field> f3 = getAllFields(TestModel.C4.class, withAnnotation(new TestModel.AF1() {
                            public String value() {return "2";}
                            public Class<? extends Annotation> annotationType() {return TestModel.AF1.class;}}));

        System.out.println();
    }
}
