package org.reflections.filters;

import com.google.common.base.Predicate;

import java.util.regex.Pattern;

/**
 *
 */
public class PatternFilter implements Predicate<String> {
    private final Pattern pattern;

    public PatternFilter(final String patternString) {
        pattern = Pattern.compile(patternString);
    }

    public boolean apply(final String name) {
        return pattern.matcher(name).matches();
    }
}
