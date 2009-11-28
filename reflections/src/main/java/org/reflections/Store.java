package org.reflections;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Supplier;
import com.google.common.collect.*;
import org.reflections.scanners.*;
import org.reflections.util.Utils;

import java.lang.annotation.Inherited;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * stores metadata information in multimaps
 * <p>use the different query methods (getXXX) to query the metadata
 * <p>the query methods are string based, and does not cause the class loader to define the types
 * <p>use {@link org.reflections.Reflections#getStore()} to access this store
 */
public class Store {

	private final Map<String/*indexName*/, Multimap<String, String>> storeMap = new MapMaker().makeComputingMap(new Function<String, Multimap<String, String>>() {
		public Multimap<String, String> apply(String indexName) {
			return Multimaps.newSetMultimap(new HashMap<String, Collection<String>>(), new Supplier<Set<String>>() {
				public Set<String> get() {
					return Sets.newHashSet();
				}
			});
		}
	});

    /** get the values of given keys stored for the given scanner class */
    public Set<String> get(Class<? extends Scanner> scannerClass, String... keys) {
        Set<String> result = Sets.newHashSet();

        Multimap<String, String> map = storeMap.get(scannerClass.getName());
        for (String key : keys) {
            result.addAll(map.get(key));
        }

        return ImmutableSet.copyOf(result);
    }

    /** return the multimap store of the given scanner class. not immutable */
	public Multimap<String, String> get(Class<? extends Scanner> scannerClass) {
        return storeMap.get(scannerClass.getName());
    }

    /** return the store map. not immutable*/
    public Map<String, Multimap<String, String>> getStoreMap() {
        return storeMap;
    }

    /** merges given store into this */
	void merge(final Store outer) {
		for (String indexName : outer.storeMap.keySet()) {
			this.storeMap.get(indexName).putAll(outer.storeMap.get(indexName));
		}
	}

    /** return the keys count */
    public Integer getKeysCount() {
        Integer keys = 0;
        for (Multimap<String, String> multimap : storeMap.values()) {
            keys += multimap.keySet().size();
        }
        return keys;
    }

    /** return the values count */
    public Integer getValuesCount() {
        Integer values = 0;
        for (Multimap<String, String> multimap : storeMap.values()) {
            values += multimap.size();
        }
        return values;
    }

    //query
    /** get sub types of a given type */
    public Set<String> getSubTypesOf(final String type) {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();

        Set<String> subTypes = get(SubTypesScanner.class, type);
        result.addAll(subTypes);

        for (String subType : subTypes) {
            result.addAll(getSubTypesOf(subType));
        }

        return result.build();
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>if given annotation is annotated with {@link java.lang.annotation.Inherited}, than inherited is honored.
     * otherwise @Inherited is not honored and all annotations are considerd inherited from interfaces and types to their subtypes
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other than a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     */
    public Set<String> getTypesAnnotatedWith(final String annotation) {
        if (isAnnotation(annotation)) {
            if (isInheritedAnnotation(annotation)) {
                return getTypesAnnotatedWithInherited(annotation);
            } else return getAllTypesAnnotatedWith(annotation);
        } else return ImmutableSet.of();
    }

    /** get types annotated with a given annotation, both classes and annotations
     * <p>@Inherited is not honored*/
    public Set<String> getAllTypesAnnotatedWith(final String annotation) {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();

        for (String typeAnnotatedWith : get(TypeAnnotationsScanner.class, annotation)) {
            result.add(typeAnnotatedWith);

            if (isAnnotation(typeAnnotatedWith)) {
                result.addAll(getTypesAnnotatedWith(annotation));
            } else if (hasSubTypes(typeAnnotatedWith)) {
                result.addAll(getSubTypesOf(typeAnnotatedWith));
            }
        }

        return result.build();
    }

    /** get types annotated with a given annotation, both classes and annotations
     * <p>@Inherited is honored*/
    public Set<String> getTypesAnnotatedWithInherited(final String annotation) {
        ImmutableSet.Builder<String> result = ImmutableSet.builder();

        if (isInheritedAnnotation(annotation)) {
            for (String typeAnnotatedWith : get(TypeAnnotationsScanner.class, annotation)) {
                result.add(typeAnnotatedWith);
                if (isClass(typeAnnotatedWith)) {
                    result.addAll(getSubTypesOf(typeAnnotatedWith));
                }
            }
        }

        return result.build();
    }

    /** get method names annotated with a given annotation */
    public Set<String> getMethodsAnnotatedWith(String annotation) {
        return ImmutableSet.copyOf(get(MethodAnnotationsScanner.class, annotation));
    }

    /** get fields annotated with a given annotation */
    public Set<String> getFieldsAnnotatedWith(String annotation) {
        return ImmutableSet.copyOf(get(FieldAnnotationsScanner.class, annotation));
    }

    /** get 'converter' methods that could effectively convert from type 'from' to type 'to' */
    public Set<String> getConverters(String from, String to) {
        return get(ConvertersScanner.class, ConvertersScanner.getConverterKey(from, to));
    }

    /** get resources relative paths where simple name (key) equals given name */
    public Set<String> getResources(final String key) {
        return get(ResourcesScanner.class, key);
    }

    /** get resources relative paths where simple name (key) matches given namePredicate */
    public Set<String> getResources(final Predicate<String> namePredicate) {
        Set<String> keys = get(ResourcesScanner.class).keySet();
        Collection<String> matches = Collections2.filter(keys, namePredicate);

        return get(ResourcesScanner.class, matches.toArray(new String[]{}));
    }

    /** get resources relative paths where simple name (key) matches given regular expression
     * <pre>Set&#60String> xmls = reflections.getResources(".*\\.xml");</pre>*/
    public Set<String> getResources(final Pattern pattern) {
        return getResources(new Predicate<String>() {
            public boolean apply(String input) {
                return pattern.matcher(input).matches();
            }
        });
    }

    //support
    /** is the given type name a class. <p>causes class loading */
    public boolean isClass(String type) {
        //todo create a string version of this
        return !isInterface(type);
    }

    /** is the given type name an interface. <p>causes class loading */
    public boolean isInterface(String aClass) {
        //todo create a string version of this
        return Utils.forName(aClass).isInterface();
    }

    /** is the given type is an annotation, based on the metadata stored by TypeAnnotationsScanner */
    public boolean isAnnotation(String typeAnnotatedWith) {
        return getTypeAnnotations().contains(typeAnnotatedWith);
    }

    /** is the given annotation an inherited annotation, based on the metadata stored by TypeAnnotationsScanner */
    public boolean isInheritedAnnotation(String typeAnnotatedWith) {
        return get(TypeAnnotationsScanner.class).get(Inherited.class.getName()).contains(typeAnnotatedWith);
    }

    /** does the given type has sub types, based on the metadata stored by SubTypesScanner */
    public boolean hasSubTypes(String typeAnnotatedWith) {
        return getSuperTypes().contains(typeAnnotatedWith);
    }

    /** get all super types that have stored sub types, based on the metadata stored by SubTypesScanner */
    public Multiset<String> getSuperTypes() {
        return get(SubTypesScanner.class).keys();
    }

    /** get all annotations, based on metadata stored by TypeAnnotationsScanner */
    public Set<String> getTypeAnnotations() {
        return get(TypeAnnotationsScanner.class).keySet();
    }
}
