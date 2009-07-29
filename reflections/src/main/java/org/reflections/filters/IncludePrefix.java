package org.reflections.filters;

/**
 * Include from scanning any name that starts with the speciifed string.
 */
public class IncludePrefix implements IncludeFilter<String> {
    private final String prefix;

    public IncludePrefix(final String prefix) {
        this.prefix = prefix;
    }

    public boolean apply(final String name) {
        return name.startsWith(prefix);
    }
}
