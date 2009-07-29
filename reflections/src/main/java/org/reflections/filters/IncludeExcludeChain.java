package org.reflections.filters;

import org.reflections.ReflectionsException;

import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

import com.google.common.collect.ImmutableList;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

/**
 *
 */
public class IncludeExcludeChain<T> implements Predicate<T> {
    private final List<IncludeExcludeFilter<T>> includeExcludeFilters;
    private final boolean startWith;

    /** an IncludeExcludeChain by given list of includeExcludeFilters
     * <p>
     * the first filter determines the starting state of the chain - that is:
     * if the first filter is IncludeFilter, than the filter starting state is a exclude all. if it's ExcludeFilter, than it's include all.
     * */
    public IncludeExcludeChain(final IncludeExcludeFilter<T>... includeExcludeFilters) {
        if (includeExcludeFilters == null || includeExcludeFilters.length == 0) {
            startWith = true;
            this.includeExcludeFilters = ImmutableList.of();
        } else {
            startWith = includeExcludeFilters[0] instanceof ExcludeFilter; //start with the opposite of the first filter
            this.includeExcludeFilters = ImmutableList.of(includeExcludeFilters);
        }
    }

    public IncludeExcludeChain(final Collection<IncludeExcludeFilter<T>> filters) {
        //noinspection unchecked
        this(filters.toArray(new IncludeExcludeFilter[filters.size()]));
    }

    /** a comma separated list of include exclude filters.
     * <p>
     * for example parse("-java., -javax., -sun., -com.sun.") or parse("+com.myn,-com.myn.excluded")
     * <p>
     * the first filter determines the starting state of the chain - that is:
     * if the first filter is IncludeFilter (starts with '+'), than the filter starting state is a exclude all. if it's ExcludeFilter (starts with '-') than it's include all.
     * */
    public static Predicate<String> parse(String includeExcludeString) {
        List<IncludeExcludeFilter<String>> filters = new ArrayList<IncludeExcludeFilter<String>>();

        if (includeExcludeString != null && !includeExcludeString.isEmpty()) {
            for (String string : includeExcludeString.split(",")) {
                String trimmed = string.trim();
                char prefix = trimmed.charAt(0);
                String pattern = trimmed.substring(1);

                IncludeExcludeFilter<String> filter;
                switch (prefix) {
                    case '+':
                        filter = new IncludePrefix(pattern);
                        break;
                    case '-':
                        filter = new ExcludePrefix(pattern);
                        break;
                    default:
                        throw new ReflectionsException("includeExclude should start with either + or -");
                }

                filters.add(filter);
            }

            return new IncludeExcludeChain<String>(filters);
        } else {
            return new IncludeExcludeChain<String>();
        }
    }

    public boolean apply(final T name) {
        boolean accept = startWith;
        for (IncludeExcludeFilter<T> filter : includeExcludeFilters) {
            //skip if this filter won't change
            if (accept && filter instanceof IncludeFilter) {
                continue;
            }
            if (!accept && filter instanceof ExcludeFilter) {
                continue;
            }

            accept = filter.apply(name);
        }

        return accept;
    }
}
