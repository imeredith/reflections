package org.reflections.util;

import com.google.common.collect.*;
import org.reflections.ReflectionsException;

import org.reflections.vfs.Vfs;

import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Some classpath convenient methods
 *
 */
public abstract class ClasspathHelper {

    /** returns urls using {@link ClassLoader#getResources(String)}
     * <p>getUrlsForName("org") effectively returns urls from classpath with packages starting with org
     */
    public static Set<URL> getUrlsForName(String name) {
        try {
            final Set<URL> result = new HashSet<URL>();

            String resourceName = name.replace(".", "/");
            final Enumeration<URL> urls = Utils.getContextClassLoader().getResources(resourceName);
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                result.add(new URL(url.toExternalForm().substring(0, url.toExternalForm().lastIndexOf(resourceName))));
            }

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** returns the url that contains the given class, using {@link ClassLoader#getResource(String)} */
    public static URL getUrlForName(Class<?> aClass) {
        try {
            final URL url = Utils.getContextClassLoader().getResource(aClass.getName().replace(".", "/") + ".class");
            return new URL(url.toExternalForm().substring(0, url.toExternalForm().lastIndexOf(aClass.getPackage().getName().replace(".", "/"))));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
    
    /** returns urls using {@link java.net.URLClassLoader#getURLs()} up the classloader parent hierarchy */
    public static Set<URL> getUrlsForClassloader() {
        Set<URL> result = Sets.newHashSet();

        //is URLClassLoader?
        ClassLoader loader = Utils.getContextClassLoader();
        while (loader != null) {
            if (loader instanceof URLClassLoader) {
                URL[] urls = ((URLClassLoader) loader).getURLs();
                if (urls != null) {
                    Collections.addAll(result, urls);
                }
            }
            loader = loader.getParent();
        }

        return result;
    }

    /** returns urls using java.class.path system property */
    public static Set<URL> getUrlsForJavaClassPath() {
        Set<URL> urls = new HashSet<URL>();

        String javaClassPath = System.getProperty("java.class.path");
        if (javaClassPath != null) {
            for (String path : javaClassPath.split(File.pathSeparator)) {
                try {
                    urls.add(new File(path).toURI().toURL());
                } catch (Exception e) {
                    throw new ReflectionsException("could not create url from " + path, e);
                }
            }
        }

        return urls;
    }

    /** returns urls using {@link ServletContext} in resource path WEB-INF/lib */
    public static Set<URL> getUrlsForWebInfLib(final ServletContext servletContext) {
        final Set<URL> urls = Sets.newHashSet();
        for (Object urlString : servletContext.getResourcePaths("/WEB-INF/lib")) {
            try { urls.add(servletContext.getResource((String) urlString)); }
            catch (MalformedURLException e) { /*fuck off*/ }
        }
        return urls;
    }

    /** returns urls using {@link ServletContext} in resource path WEB-INF/classes */
    public static URL getUrlForWebInfClasses(final ServletContext servletContext) {
        try {
            final String path = servletContext.getRealPath("/WEB-INF/classes");
            final File file = new File(path);
            if (file.exists()) return file.toURL();
        }
        catch (MalformedURLException e) { /*fuck off*/ }

        return null;
    }

    /** get the urls that are in the current class path.
     * attempts to load the jar manifest, if any, and adds to the result any dependencies it finds. */
    public static Set<URL> getUrlsForManifestsCurrentClasspath() {
        return getUrlsForManifests(getUrlsForClassloader());
    }

    /** get the urls that are specified in the manifest of the given urls.
     * attempts to load the jar manifest, if any, and adds to the result any dependencies it finds. */
    public static Set<URL> getUrlsForManifests(final Set<URL> urls) {
        Set<URL> manifestUrls = Sets.newHashSet();

        // determine if any of the URLs are JARs, and get any dependencies
        for (URL url : urls) {
            manifestUrls.addAll(getUrlsForManifest(url));
        }

        // return an immutable instance of the list
        return ImmutableSet.copyOf(manifestUrls);
    }

    /** get the urls that are specified in the manifest of the given url for a jar file.
     * attempts to load the jar manifest, if any, and adds to the result any dependencies it finds. */
    public static Set<URL> getUrlsForManifest(final URL url) {
        final Set<URL> javaClassPath = Sets.newHashSet();
        javaClassPath.add(url);

        try {
            final String part = Vfs.normalizePath(url);
            File jarFile = new File(part);
            JarFile myJar = new JarFile(part);

            URL validUrl = tryToGetValidUrl(jarFile.getPath(), new File(part).getParent(), part);
            if (validUrl != null) { javaClassPath.add(validUrl); }

            final Manifest manifest = myJar.getManifest();
            if (manifest != null) {
                final String classPath = manifest.getMainAttributes().getValue(new Attributes.Name("Class-Path"));
                if (classPath != null) {
                    for (String jar : classPath.split(" ")) {
                        validUrl = tryToGetValidUrl(jarFile.getPath(), new File(part).getParent(), jar);
                        if (validUrl != null) { javaClassPath.add(validUrl); }
                    }
                }
            }
        } catch (IOException e) {
            // don't do anything, we're going on the assumption it is a jar, which could be wrong
        }

        return javaClassPath;
    }

    //a little bit cryptic...
    private static URL tryToGetValidUrl(String workingDir, String path, String filename) {
        try {
            if (new File(filename).exists())
                return new File(filename).toURI().toURL();
            if (new File(path + File.separator + filename).exists())
                return new File(path + File.separator + filename).toURI().toURL();
            if (new File(workingDir + File.separator + filename).exists())
                return new File(workingDir + File.separator + filename).toURI().toURL();
        } catch (MalformedURLException e) {
            // don't do anything, we're going on the assumption it is a jar, which could be wrong
        }
        return null;
    }
}

