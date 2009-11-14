package org.reflections;

import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.lang.reflect.AnnotatedElement;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

//todo add some ReflectionUtils stuff here
/** convenient reflection methods */
public abstract class ReflectionUtils {

    public static <T> Collection<? extends Class<? super T>> getAllSuperTypes(final Class<T> type) {
        Collection<Class<? super T>> result = Lists.newArrayList();

        Class<? super T> superclass = type.getSuperclass();
        Class<? super T>[] interfaces = type.getInterfaces();

        Collections.addAll(result, interfaces);
        result.add(superclass);

        result = Collections2.filter(result, Predicates.notNull());

        Collection<Class<? super T>> subResult = Lists.newArrayList();
        for (Class<? super T> aClass1 : result) {
            Collection<? extends Class<? super T>> classes = getAllSuperTypes(aClass1);
            subResult.addAll(classes);
        }

        result.addAll(subResult);
        return result;
    }

    /** return all super types of a given annotated element annotated with a given annotation up in hierarchy, including the given type */
    public static List<AnnotatedElement> getAllSuperTypesAnnotatedWith(final AnnotatedElement annotatedElement, final Annotation annotation) {
        final List<AnnotatedElement> annotated = Lists.newArrayList();

        if (annotatedElement != null) {
            if (annotatedElement.isAnnotationPresent(annotation.annotationType())) {
                annotated.add(annotatedElement);
            }

            if (annotatedElement instanceof Class<?>) {
                List<AnnotatedElement> subResult = Lists.newArrayList();
                Class<?> aClass = (Class<?>) annotatedElement;
                subResult.addAll(getAllSuperTypesAnnotatedWith(aClass.getSuperclass(), annotation));
                for (AnnotatedElement anInterface : aClass.getInterfaces()) {
                    subResult.addAll(getAllSuperTypesAnnotatedWith(anInterface, annotation));
                }
                annotated.addAll(subResult);
            }
        }

        return annotated;
    }

    /**
     * checks for annotation member values matching on an annotated element or it's first annotated super type, based on equlaity of members
     * <p>override this to adopt a different annotation member values matching strategy
     */
    protected static boolean isAnnotationMembersMatcing(final Annotation annotation1, final AnnotatedElement annotatedElement) {
        List<AnnotatedElement> elementList = ReflectionUtils.getAllSuperTypesAnnotatedWith(annotatedElement, annotation1);

        if (!elementList.isEmpty()) {
            AnnotatedElement element = elementList.get(0);
            Annotation annotation2 = element.getAnnotation(annotation1.annotationType());

            if (annotation2 != null && annotation1.annotationType() == annotation2.annotationType()) {
                for (Method method : annotation1.annotationType().getDeclaredMethods()) {
                    try {
                        if (!method.invoke(annotation1).equals(method.invoke(annotation2))) {
                            return false;
                        }
                    } catch (Exception e) {
                        throw new ReflectionsException(java.lang.String.format("could not invoke method %s on annotation %s", method.getName(), annotation1.annotationType()), e);
                    }
                }
                return true;
            }
        }

        return false;
    }

    /**
     * returns a subset of given annotatedWith, where annotation member values matches the given annotation
     */
    protected static <T extends AnnotatedElement> Set<T> getMatchingAnnotations(final Set<T> annotatedElements, final Annotation annotation) {
        Set<T> result = Sets.newHashSet();

        for (AnnotatedElement annotatedElement : annotatedElements) {
            if (isAnnotationMembersMatcing(annotation, annotatedElement)) {
                //noinspection unchecked
                result.add((T) annotatedElement);
            }
        }

        return result;
    }
}
