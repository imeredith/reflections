package org.reflections.scanners;

import java.lang.annotation.Inherited;
import java.util.List;

/**
 *
 */
/** scans for class's annotations */
@SuppressWarnings({"unchecked"})
public class TypeAnnotationsScanner extends AbstractScanner {
    public void scan(final Object cls) {
		final String className = getMetadataAdapter().getClassName(cls);
		List<String> annotationTypes = getMetadataAdapter().getClassAnnotationNames(cls);
        for (String annotationType : annotationTypes) {
            if (acceptResult(annotationType)) {
                getStore().put(annotationType, className);
            }
            
            //as an exception, accept Inherited as well
            if (annotationType.equals(Inherited.class.getName())) {
                getStore().put(annotationType, className);
            }
        }
    }

}
