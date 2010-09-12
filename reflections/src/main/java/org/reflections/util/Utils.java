package org.reflections.util;

import com.google.common.collect.Lists;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * a garbage can of convenient methods
 */
public abstract class Utils {
    /** tries to resolve a java type name to a Class */
    public static Class<?> forName(String typeName) {
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

            try {
                return Class.forName(type, false, getContextClassLoader());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
    }

    //primitive parallel arrays
    public final static List<String> primitiveNames = Lists.newArrayList("boolean", "char", "byte", "short", "int", "long", "float", "double", "void");
    public final static List<Class> primitiveTypes = Lists.<Class>newArrayList(boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class, void.class);
    public final static List<String> primitiveDescriptors = Lists.newArrayList("Z", "C", "B", "S", "I", "J", "F", "D", "V");

    /** try to resolve all given string representation of types to a list of java types */
    public static <T> List<Class<? extends T>> forNames(final Iterable<String> classes) {
        List<Class<? extends T>> result = new ArrayList<Class<? extends T>>();
        for (String className : classes) {
            result.add((Class<? extends T>) forName(className));
        }
        return result;
    }

    public static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static String repeat(String string, int times) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < times; i++) {
            sb.append(string);
        }

        return sb.toString();
    }

    /**
     * isEmpty compatible with Java 5
     */
    public static boolean isEmpty(String s) {
        return s == null || s.length() == 0;
    }

    public static File prepareFile(String filename) {
        File file = new File(filename);
        File parent = file.getAbsoluteFile().getParentFile();
        if (!parent.exists()) {
            //noinspection ResultOfMethodCallIgnored
            parent.mkdirs();
        }
        return file;
    }

    public static Method getMethodFromDescriptor(String descriptor) throws ReflectionsException {
        //todo create method md

        int p0 = descriptor.indexOf('(');
        String methodKey = descriptor.substring(0, p0);
        String methodParameters = descriptor.substring(p0 + 1, descriptor.length() - 1);

        int p1 = methodKey.lastIndexOf('.');
        String className = methodKey.substring(methodKey.lastIndexOf(' ') + 1, p1);
        String methodName = methodKey.substring(p1 + 1);

        Class<?>[] parameterTypes = null;
        if (!isEmpty(methodParameters)) {
            String[] parameterNames = methodParameters.split(", ");
            List<Class<?>> result = new ArrayList<Class<?>>(parameterNames.length);
            for (String className1 : parameterNames) {
                //noinspection unchecked
                result.add(forName(className1));
            }
            List<Class<?>> types = result;
            parameterTypes = types.toArray(new Class<?>[types.size()]);
        }

        Class<?> aClass = forName(className);
        try {
            if (descriptor.contains("<init>")) {
//                return aClass.getConstructor(parameterTypes);
                return null; //todo add support
            } else {
                return aClass.getMethod(methodName, parameterTypes);
            }
        } catch (NoSuchMethodException e) {
            throw new ReflectionsException("Can't resolve method named " + methodName, e);
        }
    }

    public static Field getFieldFromString(String field) {
        //todo create field md
        String className = field.substring(0, field.lastIndexOf('.'));
        String fieldName = field.substring(field.lastIndexOf('.') + 1);

        try {
            return forName(className).getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new ReflectionsException("Can't resolve field named " + fieldName, e);
        }
    }
}
