package org.reflections.util;

import org.reflections.ReflectionsException;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** convenient methods for translating to/from type descriptors */
public class DescriptorHelper {

    /** I[Ljava.lang.String; -> int, java.lang.String[] */
    public static List<String> splitDescriptor(final String descriptor) {
        List<String> result = new ArrayList<String>();

        int cursor = 0;
        while (cursor < descriptor.length()) {
            int start = cursor;
            while (descriptor.charAt(cursor) == '[') {cursor ++;}
            char rawType = descriptor.charAt(cursor);
            if (rawType == 'L') {
                cursor = descriptor.indexOf(";", cursor);
            }
            cursor++;

            result.add(descriptorToTypeName(descriptor.substring(start, cursor)));
        }

        return result;
    }

    /** I -> int; [Ljava.lang.String; -> java.lang.String[] */
    public static String descriptorToTypeName(final String element) {
        int start = element.lastIndexOf("[") + 1;
        char rawType = element.charAt(start);

        String name;
        switch (rawType) {
            case 'L':
                name = element.substring(start + 1, element.indexOf(';')).replace("/", ".");
                break;
            default:
                name = primitiveToTypeName(rawType);
                break;
        }

        return name + Utils.repeat("[]", start);
    }
    /** int -> I; java.lang.Object -> java.lang.Object; java.lang.String[] -> [Ljava.lang.String; */
    public static String typeNameToDescriptor(String typeName) {
        if (!typeName.contains("[")) {
            return typeName;
        } else {
        String s = typeName.replaceAll("]", "");
            int i = typeName.indexOf('[');
            String arr = s.substring(s.indexOf('['));
            String s1 = typeName.substring(0, i);
            return arr + 'L' + s1 + ';';
        }
    }

    /** method (I[Ljava.lang.String;)Ljava.lang.Object; -> int, java.lang.String[] */
    public static List<String> methodDescriptorToParameterNameList(final String descriptor) {
        return splitDescriptor(
                descriptor.substring(descriptor.indexOf("(") + 1, descriptor.lastIndexOf(")")));
    }

    /** method (I[Ljava.lang.String;)Ljava.lang.Object; -> java.lang.Object */
    public static String methodDescriptorToReturnTypeName(final String descriptor) {
        return splitDescriptor(
                descriptor.substring(descriptor.lastIndexOf(")") + 1))
                .get(0);
    }

    /** I -> java.lang.Integer; V -> java.lang.Void */
    public static String primitiveToTypeName(final char rawType) {
        Class<?> primitive = rawPrimitiveToType(rawType);
        return primitive != null ? primitive.getName() : null;
    }

    /** I -> java.lang.Integer; V -> java.lang.Void */
    public static Class<?> rawPrimitiveToType(char rawType) {
        return  'Z' == rawType ? Boolean.TYPE :
                'C' == rawType ? Character.TYPE :
                'B' == rawType ? Byte.TYPE :
                'S' == rawType ? Short.TYPE :
                'I' == rawType ? Integer.TYPE :
                'J' == rawType ? Long.TYPE :
                'F' == rawType ? Float.TYPE :
                'D' == rawType ? Double.TYPE :
                'V' == rawType ? Void.TYPE :
                /*error*/      null;
    }

    //let jit inline this if neccessary
    private static Class<?> primitiveNameToType(String primitiveName) {
        return primitiveName.equals("boolean") ? Boolean.TYPE :
                primitiveName.equals("char") ? Character.TYPE :
                primitiveName.equals("byte") ? Byte.TYPE :
                primitiveName.equals("short") ? Short.TYPE :
                primitiveName.equals("int") ? Integer.TYPE :
                primitiveName.equals("long") ? Long.TYPE :
                primitiveName.equals("float") ? Float.TYPE :
                primitiveName.equals("double") ? Double.TYPE :
                primitiveName.equals("void") ? Void.TYPE : null;
    }

    /**
     * java.lang.String -> java/lang/String.class
     */
    public static String classNameToResourceName(final String className) {
        return qNameToResourceName(className) + ".class";
    }

    /**
     * java.lang.String -> java/lang/String
     */
    public static String qNameToResourceName(final String qName) {
        return qName.replace(".", "/");
    }

    /**
     * tries to resolve the given type name to a java type
     * accepted types are except for ordinary java object (java.lang.String) are primitives (int, boolean, ...) and array types (java.lang.String[][])
     */
    public static Class<?> resolveType(String typeName) throws ClassNotFoundException {
        Class<?> primitive = primitiveNameToType(typeName);
        if (primitive != null) {
            return primitive;
        } else {
            String descriptor = typeNameToDescriptor(typeName);
            return Class.forName(descriptor);
        }
    }

    //
    public static Method getMethodFromDescriptor(String descriptor) throws ReflectionsException {
        //todo create method md
        if (descriptor.contains("<init>")) {
            throw new UnsupportedOperationException(); //todo impl 
        }

        int p0 = descriptor.indexOf('(');
        String methodKey = descriptor.substring(0, p0);
        String methodParameters = descriptor.substring(p0 + 1, descriptor.length() - 1);

        int p1 = methodKey.lastIndexOf('.');
        String className = methodKey.substring(methodKey.lastIndexOf(' ') + 1, p1);
        String methodName = methodKey.substring(p1 + 1);

        Class<?>[] parameterTypes = null;
        if (!Utils.isEmpty(methodParameters)) {
            String[] parameterNames = methodParameters.split(", ");
            List<Class<?>> types = Utils.forNames(parameterNames);
            parameterTypes = types.toArray(new Class<?>[types.size()]);
        }

        try {
            return resolveType(className).getMethod(methodName, parameterTypes);
        } catch (NoSuchMethodException e) {
            throw new ReflectionsException("Can't resolve method named " + methodName, e);
        } catch (ClassNotFoundException e) {
            throw new ReflectionsException(e);
        }
    }

    public static Field getFieldFromString(String field) {
        //todo create field md
        String className = field.substring(0, field.lastIndexOf('.'));
        String fieldName = field.substring(field.lastIndexOf('.') + 1);

        try {
            return resolveType(className).getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new ReflectionsException("Can't resolve field named " + fieldName, e);
        } catch (ClassNotFoundException e) {
            throw new ReflectionsException(e);
        }
    }
}
