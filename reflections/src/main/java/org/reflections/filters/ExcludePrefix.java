package org.reflections.filters;

/**
 * Exclude from scanning any name that starts with the speciifed string.
 */
public class ExcludePrefix implements ExcludeFilter<String> {
    private final String prefix;

    public ExcludePrefix(final String prefix) {
        this.prefix = prefix;
    }

    public boolean apply(final String name) {
        return !name.startsWith(prefix);
    }
}
