package org.reflections.scanners;

import com.google.common.collect.Sets;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.reflections.util.Utils.getMethodFromDescriptor;

/** scans for methods that take one class as an argument and returns another class */ 
@SuppressWarnings({"unchecked"})
public class ConvertersScanner extends AbstractScanner {
    public void scan(final Object cls) {
        List<Object> methods = getMetadataAdapter().getMethods(cls);
        for (Object method : methods) {
            List<String> parameterNames = getMetadataAdapter().getParameterNames(method);

            if (parameterNames.size() == 1) {
                String from = parameterNames.get(0);
                String to = getMetadataAdapter().getReturnTypeName(method);

                if (!to.equals("void") && (acceptResult(from) || acceptResult(to))) {
                    String methodKey = getMetadataAdapter().getMethodFullKey(cls, method);
                    getStore().put(getConverterKey(from, to), methodKey);
                }
            }
        }
    }

    public static String getConverterKey(String from, String to) {
        return from + " to " + to;
    }

    public static String getConverterKey(Class<?> from, Class<?> to) {
        return getConverterKey(from.getName(), to.getName());
    }

    public Set<Method> getConverters(final Class<?> from, final Class<?> to) {
        Set<Method> result = Sets.newHashSet();

        for (String converter : getStore().get(getConverterKey(from, to))) {
            result.add(getMethodFromDescriptor(converter, getConfiguration().getClassLoaders()));
        }

        return result;
    }
}
