package org.reflections.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * a garbage can of convenient methods
 */
public abstract class Utils {
    /** try to resolves all given string representation of types to a list of java types */
    public static <T> List<Class<? extends T>> forNames(final Iterable<String> classes) {
        List<Class<? extends T>> result = new ArrayList<Class<? extends T>>();
        for (String className : classes) {
            //noinspection unchecked
            try {
                result.add((Class<? extends T>) DescriptorHelper.resolveType(className));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public static <T> List<Class<? extends T>> forNames(final String... classes) {
        List<Class<? extends T>> result = new ArrayList<Class<? extends T>>(classes.length);
        for (String className : classes) {
            //noinspection unchecked
            try {
                result.add((Class<? extends T>) DescriptorHelper.resolveType(className));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    public static ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    public static String repeat(String string ,int times) {
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
}
