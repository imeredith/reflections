package org.reflections.scanners;

import java.util.List;

/**
 *
 */
@SuppressWarnings({"unchecked"})
/** scans for class's annotations */
public class ClassAnnotationsScanner extends AbstractScanner {
    public static final String indexName = "ClassAnnotations";

    public void scan(final Object cls) {
		final String className = getMetadataAdapter().getClassName(cls);
		List<String> annotationTypes = getMetadataAdapter().getClassAnnotationNames(cls);
        for (String annotationType : annotationTypes) {
            if (accept(annotationType)) {
                store.put(annotationType, className);
            }
        }
    }

    public String getIndexName() {
        return indexName;
    }
}
