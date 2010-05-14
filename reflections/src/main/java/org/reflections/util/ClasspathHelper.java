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

    /**
     * returns a set of urls that contain resources with prefix as the given parameter, that is exist in
     * the equivalent directory within the urls of current classpath
     */
    public static Set<URL> getUrlsForPackagePrefix(String packagePrefix) {
        try {
            Enumeration<URL> urlEnumeration = Utils.getContextClassLoader().getResources(
                    packagePrefix.replace(".", "/"));
            List<URL> urls = Lists.newArrayList(Iterators.forEnumeration(urlEnumeration));

            return getBaseUrls(urls, getUrlsForCurrentClasspath());

        } catch (IOException e) {
            throw new ReflectionsException("Can't resolve URL for package prefix " + packagePrefix, e);
        }
    }

    public static Set<URL> getUrlsForCurrentClasspath() {
        Set<URL> urls = Sets.newHashSet();

        //is URLClassLoader?
        ClassLoader loader = Utils.getContextClassLoader();
        while (loader != null) {
            if (loader instanceof URLClassLoader) {
                Collections.addAll(urls, ((URLClassLoader) loader).getURLs());
            }
            loader = loader.getParent();
        }

        if (urls.isEmpty()) {
            //get from java.class.path
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
        }

        return ImmutableSet.copyOf(urls);
    }

    public static Set<URL> getUrlsForWebInfLib(final ServletContext servletContext) {
        final Set<URL> urls = Sets.newHashSet();
        for (Object urlString : servletContext.getResourcePaths("/WEB-INF/lib")) {
            try { urls.add(servletContext.getResource((String) urlString)); }
            catch (MalformedURLException e) { /*fuck off*/ }
        }
        return urls;
    }

    public static URL getUrlForServletContextClasses(final ServletContext servletContext) {
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
        return getUrlsForManifests(getUrlsForCurrentClasspath());
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
            final String part = Vfs.normalizePath(url.getFile());
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
                return new File(filename).toURL();
            if (new File(path + File.separator + filename).exists())
                return new File(path + File.separator + filename).toURL();
            if (new File(workingDir + File.separator + filename).exists())
                return new File(workingDir + File.separator + filename).toURL();
        } catch (MalformedURLException e) {
            // don't do anything, we're going on the assumption it is a jar, which could be wrong
        }
        return null;
    }

    /**
     * the url that contains the given class.
     */
    public static URL getUrlForClass(Class<?> aClass) {
        String resourceName = aClass.getName().replace(".", "/") + ".class";
        URL packageUrl = Utils.getContextClassLoader().getResource(resourceName);
        if (packageUrl != null) {
            return getBaseUrl(packageUrl, getUrlsForCurrentClasspath());
        } else {
            return null;
        }
    }

    /** get's the base url from the given urls */
    public static URL getBaseUrl(final URL url, final Collection<URL> baseUrls) {
        if (url != null) {
            String path1 = Vfs.normalizePath(url);

            //try to return the base url
            for (URL baseUrl : baseUrls) {
                String path2 = Vfs.normalizePath(baseUrl);
                if (path1.startsWith(path2)) {
                    return baseUrl;
                }
            }
        }

        return url;
    }

    /** get's the base url from urls in current classpath */
    public static URL getBaseUrl(final URL url) {
        return getBaseUrl(url, getUrlsForCurrentClasspath());
    }

    /** get's the base urls from the given urls */
    public static Set<URL> getBaseUrls(final List<URL> urls, final Collection<URL> baseUrls) {
        Set<URL> result = Sets.newHashSet();
        for (URL url : urls) {
            result.add(getBaseUrl(url, baseUrls));
        }
        return result;
    }
}

