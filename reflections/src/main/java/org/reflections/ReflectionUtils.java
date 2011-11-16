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
/** convenient reflection methods
 * <p>
 *     some helper methods to get superTypes, methods, fields matching some predicates, generally:
 *     <pre>
 *     Set&#60?> result = getAllXXX(type/s, withYYY)
 *     Set&#60?> result = getAllXXX(type/s, Predicates.and(withYYY, withZZZ))
 *     </pre>
 *     see {@link ReflectionUtils#getAllSuperTypes(Class, com.google.common.base.Predicate)},
 *     {@link ReflectionUtils#getAllFields(Class, com.google.common.base.Predicate)},
 *     {@link ReflectionUtils#getAllMethods(Iterable, com.google.common.base.Predicate)},
 *     {@link ReflectionUtils#getAll(Iterable, com.google.common.base.Predicate)}
 *     <p>predicates included here all starts with "with", for example {@link ReflectionUtils#withAnnotation(java.lang.annotation.Annotation)}, {@link ReflectionUtils#withModifier(int)}, {@link ReflectionUtils#withParametersAssignableFrom(Class[])}
 *
 * */
public abstract class ReflectionUtils {

    //primitive parallel arrays //todo lazy static init
    public final static List<String> primitiveNames = Lists.newArrayList("boolean", "char", "byte", "short", "int", "long", "float", "double", "void");
    @SuppressWarnings({"unchecked"}) public final static List<Class> primitiveTypes = Lists.<Class>newArrayList(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);
    public final static List<String> primitiveDescriptors = Lists.newArrayList("Z", "C", "B", "S", "I", "J", "F", "D", "V");

    /** get all super types of given {@code type}, including, filtered by {@code predicate}
     * <p>for example:
     * <pre>
     * Set&#60Class&#60?>> annotatedSuperTypes =
     *      getAllSuperTypes(type, withAnnotation(annotation));
     * </pre>*/
    public static Set<Class<?>> getAllSuperTypes(final Class<?> type, Predicate<? super Class<?>> predicate) {
        Set<Class<?>> result = Sets.newHashSet();
        if (type != null) {
            result.add(type);
            result.addAll(getAllSuperTypes(type.getSuperclass(), Predicates.alwaysTrue()));
            for (Class<?> inter : type.getInterfaces()) {
                result.addAll(getAllSuperTypes(inter, Predicates.alwaysTrue()));
            }
        }
        return Sets.newHashSet(Iterables.filter(result, predicate));
    }

    public static Set<Class<?>> getAllSuperTypes(final Iterable<Class<?>> types, Predicate<? super Class<?>> predicate) {
        Set<Class<?>> result = Sets.newHashSet(); for (Class<?> type : types) result.addAll(getAllSuperTypes(type, predicate));
        return result;
    }

    /** get all fields of given {@code type}, including, filtered by {@code predicate}
     * <p>for example:
     * <pre>
     * Set&#60Field&#60?>> injectables =
     *      getAllFields(type,
     *          Predicates.or(
     *              withAnnotation(Inject.class),
     *              withAnnotation(Autowired.class)));
     * </pre>*/
    public static Set<Field> getAllFields(final Class<?> type, Predicate<? super Field> predicate) {
        Set<Field> result = Sets.newHashSet();
        for (Class<?> t : getAllSuperTypes(type, Predicates.alwaysTrue())) {
            Collections.addAll(result, t.getDeclaredFields());
        }
        return Sets.newHashSet(Iterables.filter(result, predicate));
    }

    public static Set<Field> getAllFields(final Iterable<Class<?>> types, Predicate<? super Field> predicate) {
        Set<Field> result = Sets.newHashSet(); for (Class<?> type : types) result.addAll(getAllFields(type, predicate));
        return result;
    }

    /** get all methods of given {@code type}, including, filtered by {@code predicate}
     * <p>for example:
     * <pre>
     * Set&#60Method> getters =
     *      getAllMethods(someClasses,
     *          Predicates.and(
     *              withModifier(Modifier.PUBLIC),
     *              withPrefix("get"),
     *              withParametersCount(0)));
     * </pre>*/
    public static Set<Method> getAllMethods(final Class<?> type, Predicate<? super Method> predicate) {
        Set<Method> result = Sets.newHashSet();
        for (Class<?> t : getAllSuperTypes(type, Predicates.alwaysTrue())) {
            Collections.addAll(result, t.isInterface() ? t.getMethods() : t.getDeclaredMethods());
        }

        return Sets.newHashSet(Iterables.filter(result, predicate));
    }

    /** get all methods of given {@code types}, filtered by {@code predicate}*/
    public static Set<Method> getAllMethods(final Iterable<Class<?>> types, Predicate<? super Method> predicate) {
        Set<Method> result = Sets.newHashSet(); for (Class<?> type : types) result.addAll(getAllMethods(type, predicate));
        return Sets.newHashSet(result);
    }

    /** filter all given {@code elements} with {@code predicate} */
    public static <T extends AnnotatedElement> Set<T> getAll(final Iterable<T> elements, Predicate<? super T> predicate) {
        return Sets.newHashSet(Iterables.filter(elements, predicate));
    }

    /** where member name equals given {@code name} */
    public static <T extends Member> Predicate<T> withName(final String name) {
        return new Predicate<T>() {
            public boolean apply(@Nullable T input) {
                return input != null && input.getName().equals(name);
            }
        };
    }

    /** where member name startsWith given {@code prefix} */
    public static <T extends Member> Predicate<T> withPrefix(final String prefix) {
        return new Predicate<T>() {
            public boolean apply(@Nullable T input) {
                return input != null && input.getName().startsWith(prefix);
            }
        };
    }

    /** where element is annotated with given {@code annotation} */
    public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Class<? extends Annotation> annotation) {
        return new Predicate<T>() {
            public boolean apply(@Nullable T input) {
                return input != null && input.isAnnotationPresent(annotation);
            }
        };
    }

    /** where element is annotated with given {@code annotation}, including member matching */
    public static <T extends AnnotatedElement> Predicate<T> withAnnotation(final Annotation annotation) {
        return new Predicate<T>() {
            public boolean apply(@Nullable T input) {
                return input != null && input.isAnnotationPresent(annotation.annotationType()) &&
                        areAnnotationMembersMatching(input.getAnnotation(annotation.annotationType()), annotation);
            }
        };
    }

    /** when method parameter types equals given {@code types} */
    public static Predicate<Method> withParameters(final Class... types) {
        return new Predicate<Method>() {
            public boolean apply(@Nullable Method input) {
                return input != null && Arrays.equals(input.getParameterTypes(), types);
            }
        };
    }

    /** when method parameter types assignable from given {@code types} */
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

    /** when method parameter types count equal given {@code count} */
    public static Predicate<Method> withParametersCount(final int count) {
        return new Predicate<Method>() {
            public boolean apply(@Nullable Method input) {
                return input != null && input.getParameterTypes().length == count;
            }
        };
    }

    /** when field type equal given {@code type} */
    public static <T> Predicate<Field> withType(final Class<T> type) {
        return new Predicate<Field>() {
            public boolean apply(@Nullable Field input) {
                return input != null && input.getType().equals(type);
            }
        };
    }

    /** when field type assignable from given {@code type} */
    public static <T> Predicate<Field> withTypeAssignableFrom(final Class<T> type) {
        return new Predicate<Field>() {
            public boolean apply(@Nullable Field input) {
                return input != null && input.getType().isAssignableFrom(type);
            }
        };
    }

    /** when method return type equal given {@code type} */
    public static <T> Predicate<Method> withReturnType(final Class<T> type) {
        return new Predicate<Method>() {
            public boolean apply(@Nullable Method input) {
                return input != null && input.getReturnType().equals(type);
            }
        };
    }

    /** when method return type assignable from given {@code type} */
    public static <T> Predicate<Method> withReturnTypeAssignableFrom(final Class<T> type) {
        return new Predicate<Method>() {
            public boolean apply(@Nullable Method input) {
                return input != null && input.getReturnType().isAssignableFrom(type);
            }
        };
    }

    /** when member modifier matches given {@code mod}
     * <p>for example:
     * <pre>
     * withModifier(Modifier.PUBLIC)
     * withModifier(Modifier.PROTECTED | Modifier.PUBLIC)
     * </pre>
     */
    public static <T extends Member> Predicate<T> withModifier(final int mod) {
        return new Predicate<T>() {
            public boolean apply(@Nullable T input) {
                return input != null && (input.getModifiers() & mod) != 0;
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
     * <p>if optional {@link ClassLoader}s are not specified, then both {@link org.reflections.util.ClasspathHelper#contextClassLoader()} and {@link org.reflections.util.ClasspathHelper#staticClassLoader()} are used
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

    public static List<String> names(Iterable<Class<?>> types) {
        List<String> result = Lists.newArrayList();
        for (Class<?> type : types) {
            result.add(type.getName());
        }
        return result;
    }
}
