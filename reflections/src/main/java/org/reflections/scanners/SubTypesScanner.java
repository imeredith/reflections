package org.reflections.scanners;

import java.util.List;

/** scans for superclass and interfaces of a class, allowing a reverse lookup for subtypes */
public class SubTypesScanner extends AbstractScanner {
    @SuppressWarnings({"unchecked"})
    public void scan(final Object cls) {
		String className = getMetadataAdapter().getClassName(cls);
		String superclass = getMetadataAdapter().getSuperclassName(cls);
		List<String> interfaces = getMetadataAdapter().getInterfacesNames(cls);

		if (!Object.class.getName().equals(superclass) && acceptResult(superclass)) {
            getStore().put(superclass, className);
        }

		for (String anInterface : interfaces) {
			if (acceptResult(anInterface)) {
                getStore().put(anInterface, className);
            }
        }
    }
}
