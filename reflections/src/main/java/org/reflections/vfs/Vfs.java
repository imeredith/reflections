package org.reflections.vfs;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.reflections.ReflectionsException;
import org.reflections.util.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

/**
 * a simple virtual file system bridge
 * <p>use the {@link org.reflections.vfs.Vfs#fromURL(java.net.URL)} to get a {@link org.reflections.vfs.Vfs.Dir}
 * and than use {@link org.reflections.vfs.Vfs.Dir#getFiles()} to iterate over it's {@link org.reflections.vfs.Vfs.File}
 * <p>use {@link org.reflections.vfs.Vfs#findFiles(java.util.List, com.google.common.base.Predicate)} to get an
 * iteration of matching files over a url
 * <p>{@link org.reflections.vfs.Vfs#fromURL(java.net.URL)} uses static {@link org.reflections.vfs.Vfs.DefaultUrlTypes} to resolves URLs
 * and it can be plugged in with {@link org.reflections.vfs.Vfs#setDefaultURLTypes(java.util.List)}.
 */
public abstract class Vfs {
    private static List<UrlType> defaultUrlTypes = Lists.<UrlType>newArrayList(DefaultUrlTypes.values());

    /** an abstract vfs dir */
    public interface Dir {
        String getPath();
        Iterable<File> getFiles();
        void close();
    }

    /** an abstract vfs file */
    public interface File {
        String getName();
        String getRelativePath();
        String getFullPath();
        InputStream getInputStream() throws IOException;
    }

    /** a matcher and factory for a url */
    public interface UrlType {
        boolean matches(URL url);
        Dir createDir(URL url);
    }

    /** the default url types that will be used when issuing {@link org.reflections.vfs.Vfs#fromURL(java.net.URL)} */
    public static List<UrlType> getDefaultUrlTypes() {
        return defaultUrlTypes;
    }

    /** sets the static default url types. can be used to statically plug in urlTypes */
    public static void setDefaultURLTypes(final List<UrlType> urlTypes) {
        defaultUrlTypes = urlTypes;
    }

    /** add a static default url types. can be used to statically plug in urlTypes */
    public static void addDefaultURLTypes(UrlType urlType) {
        defaultUrlTypes.add(urlType);
    }

    /** tries to create a Dir from the given url, using the defaultUrlTypes */
    public static Dir fromURL(final URL url) {
        return fromURL(url, defaultUrlTypes);
    }

    /** tries to create a Dir from the given url, using the given urlTypes*/
    public static Dir fromURL(final URL url, final List<UrlType> urlTypes) {
        for (UrlType type : urlTypes) {
            if (type.matches(url)) {
                try {
                    return type.createDir(url);
                } catch (Exception e) {
                    throw new ReflectionsException("could not create Dir using " + type.getClass().getName() +" from url " + url.toExternalForm());
                }
            }
        }

        throw new ReflectionsException("could not create Dir from url, no matching UrlType was found [" + url.toExternalForm() + "]\n" +
                "either use fromURL(final URL url, final List<UrlType> urlTypes) or " +
                "use the static setDefaultURLTypes(final List<UrlType> urlTypes) or addDefaultURLTypes(UrlType urlType) " +
                "with your specialized UrlType.");
    }

    /** tries to create a Dir from the given url, using the given urlTypes*/
    public static Dir fromURL(final URL url, final UrlType... urlTypes) {
        return fromURL(url, Lists.<UrlType>newArrayList(urlTypes));
    }

    /** return an iterable of all {@link org.reflections.vfs.Vfs.File} in given urls, matching filePredicate */
    public static Iterable<File> findFiles(final List<URL> inUrls, final Predicate<File> filePredicate) {
        Iterable<File> result = null;

        for (URL url : inUrls) {
            Iterable<File> iterable = Iterables.filter(fromURL(url).getFiles(), filePredicate);
            result = result == null ? iterable : Iterables.concat(result, iterable);
        }

        return result;
    }

    /** return an iterable of all {@link org.reflections.vfs.Vfs.File} in given urls, starting with given packagePrefix and matching nameFilter */
    public static Iterable<File> findFiles(final List<URL> inUrls, final String packagePrefix, final Predicate<String> nameFilter) {
        Predicate<File> fileNamePredicate = new Predicate<File>() {
            public boolean apply(File file) {
                String path = file.getRelativePath();
                if (path.startsWith(packagePrefix)) {
                    String filename = path.substring(path.indexOf(packagePrefix) + packagePrefix.length());
                    return !Utils.isEmpty(filename) && nameFilter.apply(filename.substring(1));
                } else {
                    return false;
                }
            }
        };

        return findFiles(inUrls, fileNamePredicate);
    }

    public static enum DefaultUrlTypes implements UrlType {
        jarfile {
            public boolean matches(URL url) {return url.getProtocol().equals("file") && url.toExternalForm().endsWith(".jar");}
            public Dir createDir(final URL url) {return new ZipDir(url);}},

        jarUrl {
            public boolean matches(URL url) {return url.toExternalForm().contains(".jar!");}
            public Dir createDir(URL url) {return new ZipDir(url);}},

        directory {
            public boolean matches(URL url) {return url.getProtocol().equals("file") && new java.io.File(normalizePath(url)).isDirectory();}
            public Dir createDir(final URL url) {return new SystemDir(url);}}
    }

    //
    //todo remove this method?
    public static String normalizePath(final URL url) {
        return normalizePath(url.toExternalForm());
    }

    //todo remove this method?
    //todo this should be removed and normaliztion should happen per UrlType and it is it's responsibility
    public static String normalizePath(final String urlPath) {
        String path = urlPath;

        path = path.replace("/", java.io.File.separator); //normalize separators
        path = path.replace("\\", java.io.File.separator); //normalize separators
        while (path.contains("//")) {path = path.replaceAll("//", "/");} //remove multiple slashes
        if (path.contains(":")) { //remove protocols
            String[] protocols = path.split(":");
            if (protocols.length > 1) {
                String maybeDrive = protocols[protocols.length - 2];
                String lastSegment = protocols[protocols.length - 1];
                if (maybeDrive.length() == 1) {
                    //leave the windows drive character if exists
                    path = maybeDrive.toLowerCase() + ":" + lastSegment;
                } else {
                    path = lastSegment;
                }
            }
        }
        if (path.contains("!")) {path = path.substring(0, path.lastIndexOf("!"));} //remove jar ! sign
        while(path.endsWith("/")) {path = path.substring(0, path.length() - 1);} //remove extra / at the end

        return path;
    }
}
