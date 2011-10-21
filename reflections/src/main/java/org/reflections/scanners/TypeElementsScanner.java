package org.reflections.scanners;

/** scans fields and methods and stores fqn as key and elements as values */
@SuppressWarnings({"unchecked"})
public class TypeElementsScanner extends AbstractScanner {
    private boolean includeFields = true;
    private boolean includeMethods = true;
    private boolean publicOnly = true;

    public void scan(Object cls) {
        //avoid scanning JavaCodeSerializer outputs
        if (TypesScanner.isJavaCodeSerializer(getMetadataAdapter().getInterfacesNames(cls))) return;

        String className = getMetadataAdapter().getClassName(cls);

        if (includeFields) {
            for (Object field : getMetadataAdapter().getFields(cls)) {
                String fieldName = getMetadataAdapter().getFieldName(field);
                getStore().put(className, fieldName);
            }
        }

        if (includeMethods) {
            for (Object method : getMetadataAdapter().getMethods(cls)) {
                if (!publicOnly || getMetadataAdapter().isPublic(method)) {
                    getStore().put(className, getMetadataAdapter().getMethodKey(cls, method));
                }
            }
        }
    }

    //
    public TypeElementsScanner includeFields() { return includeFields(true); }
    public TypeElementsScanner includeFields(boolean include) { includeFields = include; return this; }
    public TypeElementsScanner includeMethods() { return includeMethods(true); }
    public TypeElementsScanner includeMethods(boolean include) { includeMethods = include; return this; }
    public TypeElementsScanner publicOnly(boolean only) { publicOnly = only; return this; }
    public TypeElementsScanner publicOnly() { return publicOnly(true); }
}
