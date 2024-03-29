package org.reflections;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.reflections.Reflections.*;

/**
 * @author mamo
 */
public class ReflectionUtilsTest {

    @Test public void test() {
        //Reflections.getAllXXX(Class, withYYY)
        //Reflections.getAllXXX(Class, Predicates.and(withYYY(..), withZZZ(..), ...)

        List<Class<?>> someClasses = Lists.newArrayList();
        Set<Method> getters = getAllMethods(someClasses, Predicates.and(withModifier(Modifier.PUBLIC), withPrefix("get"), withParametersCount(0)));

        Set<Field> f1 = getAllFields(TestModel.C4.class, withName("f1"));
        Set<Field> f2 = getAllFields(TestModel.C4.class, withAnnotation(TestModel.AF1.class));
        Set<Field> f3 = getAllFields(TestModel.C4.class, withAnnotation(new TestModel.AF1() {
                            public String value() {return "2";}
                            public Class<? extends Annotation> annotationType() {return TestModel.AF1.class;}}));
        Set<Field> f4 = getAllFields(TestModel.C4.class, withTypeAssignableFrom(String.class));

        Set<Method> m1 = getAllMethods(TestModel.C4.class, Predicates.and(withParametersAssignableFrom(), withModifier(Modifier.PUBLIC)));
        Set<Method> m2 = getAllMethods(TestModel.C4.class, withParametersAssignableFrom(int.class, String.class));
        Set<Method> m3 = getAllMethods(TestModel.C4.class, withParametersAssignableFrom(int.class, String[].class));
        Set<Method> m4 = getAllMethods(TestModel.C4.class, withParametersAssignableFrom(int.class, Object.class));

        Set<Method> m5 = getAllMethods(TestModel.C4.class, withReturnType(String.class));
        Set<Method> m6 = getAllMethods(TestModel.C4.class, withReturnTypeAssignableFrom(String.class));

    }
}
