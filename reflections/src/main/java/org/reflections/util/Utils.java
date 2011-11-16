package org.reflections.util;

import org.reflections.ReflectionUtils;
import org.reflections.ReflectionsException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * a garbage can of convenient methods
 */
public abstract class Utils {

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

    public static boolean isEmpty(Object[] objects) {
        return objects == null || objects.length == 0;
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

    public static Method getMethodFromDescriptor(String descriptor, ClassLoader... classLoaders) throws ReflectionsException {
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
                result.add(ReflectionUtils.forName(className1));
            }
            parameterTypes = result.toArray(new Class<?>[result.size()]);
        }

        Class<?> aClass = ReflectionUtils.forName(className, classLoaders);
        try {
            if (descriptor.contains("<init>")) {
//                return aClass.getConstructor(parameterTypes);
                return null; //todo add support
            } else {
                return aClass.getDeclaredMethod(methodName, parameterTypes);
            }
        } catch (NoSuchMethodException e) {
            throw new ReflectionsException("Can't resolve method named " + methodName, e);
        }
    }

    public static Field getFieldFromString(String field, ClassLoader... classLoaders) {
        //todo create field md
        String className = field.substring(0, field.lastIndexOf('.'));
        String fieldName = field.substring(field.lastIndexOf('.') + 1);

        try {
            return ReflectionUtils.forName(className, classLoaders).getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new ReflectionsException("Can't resolve field named " + fieldName, e);
        }
    }

    public static void close(InputStream inputStream) {
        try { if (inputStream != null) inputStream.close(); }
        catch (IOException e) { e.printStackTrace(); }
    }

    //from sun.net.www.ParseUtil
    /*
     * flag indicates whether path uses platform dependent
     * File.separatorChar or not. True indicates path uses platform
     * dependent File.separatorChar.
     */
    public static String encodePath(String path, boolean flag) {
        char[] retCC = new char[path.length() * 2 + 16];
        int retLen = 0;
        char[] pathCC = path.toCharArray();

        int n = path.length();
        for (int i = 0; i < n; i++) {
            char c = pathCC[i];
            if ((!flag && c == '/') || (flag && c == File.separatorChar)) retCC[retLen++] = '/';
            else if (c <= 0x007F)
                if (c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9') retCC[retLen++] = c;
                else if (getEncodedInPath().get(c)) retLen = escape(retCC, c, retLen);
                else retCC[retLen++] = c;
            else if (c > 0x07FF) {
                retLen = escape(retCC, (char) (0xE0 | ((c >> 12) & 0x0F)), retLen);
                retLen = escape(retCC, (char) (0x80 | ((c >> 6) & 0x3F)), retLen);
                retLen = escape(retCC, (char) (0x80 | ((c >> 0) & 0x3F)), retLen);
            } else {
                retLen = escape(retCC, (char) (0xC0 | ((c >> 6) & 0x1F)), retLen);
                retLen = escape(retCC, (char) (0x80 | ((c >> 0) & 0x3F)), retLen);
            }
            //worst case scenario for character [0x7ff-] every single
            //character will be encoded into 9 characters.
            if (retLen + 9 > retCC.length) {
                int newLen = retCC.length * 2 + 16;
                if (newLen < 0) newLen = Integer.MAX_VALUE;
                char[] buf = new char[newLen];
                System.arraycopy(retCC, 0, buf, 0, retLen);
                retCC = buf;
            }
        }
        return new String(retCC, 0, retLen);
    }

    /**
     * Appends the URL escape sequence for the specified char to the
     * specified StringBuffer.
     */
    private static int escape(char[] cc, char c, int index) {
        cc[index++] = '%';
        cc[index++] = Character.forDigit((c >> 4) & 0xF, 16);
        cc[index++] = Character.forDigit(c & 0xF, 16);
        return index;
    }

    private static BitSet encodedInPath;
    private static BitSet getEncodedInPath() {
        if (encodedInPath == null) {
            encodedInPath = new BitSet(256);
            // Set the bits corresponding to characters that are encoded in the
            // path component of a URI.
            set(encodedInPath,
            // These characters are reserved in the path segment as described in
            // RFC2396 section 3.3.
            '=', ';', '?', '/',
            // These characters are defined as excluded in RFC2396 section 2.4.3
            // and must be escaped if they occur in the data part of a URI.
            '#', ' ', '<', '>', '%', '"', '{', '}', '|', '\\', '^', '[', ']', '`');
            // US ASCII control characters 00-1F and 7F.
            for (int i = 0; i < 32; i++) encodedInPath.set(i);
            encodedInPath.set(127);
        }
        return encodedInPath;
    }

    private static void set(final BitSet encodedInPath, final char... chars) {
        for (char aChar : chars) {
            encodedInPath.set(aChar);
        }
    }
}
