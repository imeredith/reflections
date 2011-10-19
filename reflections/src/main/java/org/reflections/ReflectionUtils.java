package org.reflections;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.reflections.util.ClasspathHelper;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.*;

//todo add some ReflectionUtils stuff here
/** convenient reflection methods */
public abstract class ReflectionUtils {

    //primitive parallel arrays
    public final static List<String> primitiveNames = Lists.newArrayList("boolean", "char", "byte", "short", "int", "long", "float", "double", "void");
    @SuppressWarnings({"unchecked"}) public final static List<Class> primitiveTypes = Lists.<Class>newArrayList(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);
    public final static List<String> primitiveDescriptors = Lists.newArrayList("Z", "C", "B", "S", "I", "J", "F", "D", "V");

    public static <T extends AnnotatedElement> Set<T> getAll(final Iterable<T> elements, Predicate<? super T>... predicates) {
        return predicates != null ? Sets.newHashSet(Iterables.filter(elements, Predicates.and(predicates))) : Sets.newHashSet(elements);
    }

    /** get all super types of given types, including the types */
    public static Set<Class<?>> getAllSuperTypes(final Class<?> type, Predicate<? super Class<?>>... predicates) {
        Set<Class<?>> result = Sets.newHashSet();
        if (type != null) {
            result.add(type);
            result.addAll(getAllSuperTypes(type.getSuperclass()));
            for (Class<?> inter : type.getInterfaces()) {
                result.addAll(getAllSuperTypes(inter));
            }
        }
        if (predicates != null) {
            result = Sets.newHashSet(Iterables.filter(result, Predicates.and(predicates)));
        }
        return result;
    }

    public static Set<Field> getAllFields(final Class<?> type, Predicate<? super Field>... predicates) {
        Set<Field> result = Sets.newHashSet();
        for (Class<?> t : getAllSuperTypes(type)) {
            Collections.addAll(result, t.getDeclaredFields());
        }
        if (predicates != null) {
            result = Sets.newHashSet(Iterables.filter(result, Predicates.and(predicates)));
        }
        return result;
    }

    public static Set<Method> getAllMethods(final Class<?> type, Predicate<? super Method>... predicates) {
        Set<Method> result = Sets.newHashSet();
        for (Class<?> t : getAllSuperTypes(type)) {
            Collections.addAll(result, t.isInterface() ? t.getMethods() : t.getDeclaredMethods());
        }
        if (predicates != null) {
            result = Sets.newHashSet(Iterables.filter(result, Predicates.and(predicates)));
        }
        return result;
    }

    //
    public static <T extends Member> Predicate<T> withName(final String name) {
        return new Predicate<T>() {
            public boolean apply(@Nullable T input) {
                return input != null && input.getName().equals(name);
            }
        };
    }

    public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Class<? extends Annotation> annotation) {
        return new Predicate<T>() {
            public boolean apply(@Nullable T input) {
                return input != null && input.isAnnotationPresent(annotation);
            }
        };
    }

    public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Annotation annotation) {
        return new Predicate<T>() {
            public boolean apply(@Nullable T input) {
                return input != null && input.isAnnotationPresent(annotation.annotationType()) &&
                        areAnnotationMembersMatching(input.getAnnotation(annotation.annotationType()), annotation);
            }
        };
    }

    public static Predicate<Method> withParameters(final Class... types) {
        return new Predicate<Method>() {
            public boolean apply(@Nullable Method input) {
                return input != null && Arrays.equals(input.getParameterTypes(), types);
            }
        };
    }

    public static Predicate<Method> withParametersAssignableFrom(final Class... types) {
        return new Predicate<Method>() {
            public boolean apply(@Nullable Method input) {
                if (input != null) {
                    Class<?>[] parameterTypes = input.getParameterTypes();
                    if (parameterTypes.length == types.length) {
                        for (int i = 0; i < parameterTypes.length; i++) {
                            if (!parameterTypes[i].isAssignableFrom(types[i])) {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        };
    }

    public static Predicate<Method> withParametersCount(final int count) {
        return new Predicate<Method>() {
            public boolean apply(@Nullable Method input) {
                return input != null && input.getParameterTypes().length == count;
            }
        };
    }

    public static <T> Predicate<Field> withType(final Class<T> type) {
        return new Predicate<Field>() {
            public boolean apply(@Nullable Field input) {
                return input != null && input.getType().equals(type);
            }
        };
    }

    public static <T> Predicate<Field> withTypeAssignableFrom(final Class<T> type) {
        return new Predicate<Field>() {
            public boolean apply(@Nullable Field input) {
                return input != null && input.getType().isAssignableFrom(type);
            }
        };
    }

    /** checks for annotation member values matching, based on equality of members */
    public static boolean areAnnotationMembersMatching(Annotation annotation1, Annotation annotation2) {
        if (annotation2 != null && annotation1.annotationType() == annotation2.annotationType()) {
            for (Method method : annotation1.annotationType().getDeclaredMethods()) {
                try {
                    if (!method.invoke(annotation1).equals(method.invoke(annotation2))) {
                        return false;
                    }
                } catch (Exception e) {
                    throw new ReflectionsException(String.format("could not invoke method %s on annotation %s", method.getName(), annotation1.annotationType()), e);
                }
            }
            return true;
        }

        return false;
    }

    /**
     * returns a subset of given annotatedWith, where annotation member values matches the given annotation
     */
    public static <T extends AnnotatedElement> Set<T> getMatchingAnnotations(final Set<T> annotatedElements, final Annotation annotation) {
        Set<T> result = Sets.newHashSet();

        for (T annotatedElement : annotatedElements) {
            Annotation annotation1 = annotatedElement.getAnnotation(annotation.annotationType());
            if (areAnnotationMembersMatching(annotation, annotation1)) {
                result.add(annotatedElement);
            }
        }

        return result;
    }

    /** tries to resolve a java type name to a Class
     * <p>if optional {@link ClassLoader}s are not specified, then both {@link org.reflections.util.ClasspathHelper#getContextClassLoader()} and {@link org.reflections.util.ClasspathHelper#getStaticClassLoader()} are used
     * */
    public static Class<?> forName(String typeName, ClassLoader... classLoaders) {
        if (primitiveNames.contains(typeName)) {
            return primitiveTypes.get(primitiveNames.indexOf(typeName));
        } else {
            String type;
            if (typeName.contains("[")) {
                int i = typeName.indexOf("[");
                type = typeName.substring(0, i);
                String array = typeName.substring(i).replace("]", "");

                if (primitiveNames.contains(type)) {
                    type = primitiveDescriptors.get(primitiveNames.indexOf(type));
                } else {
                    type = "L" + type + ";";
                }

                type = array + type;
            } else {
                type = typeName;
            }

            for (ClassLoader classLoader : ClasspathHelper.classLoaders(classLoaders)) {
                try { return Class.forName(type, false, classLoader); }
                catch (ClassNotFoundException e) { /*continue*/ }
            }

            return null;
        }
    }

    /** try to resolve all given string representation of types to a list of java types */
    public static <T> List<Class<? extends T>> forNames(final Iterable<String> classes, ClassLoader... classLoaders) {
        List<Class<? extends T>> result = new ArrayList<Class<? extends T>>();
        for (String className : classes) {
            //noinspection unchecked
            result.add((Class<? extends T>) forName(className, classLoaders));
        }
        return result;
    }
}
