package org.reflections;

import org.junit.Assert;
import org.junit.Test;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.Utils;
import org.reflections.vfs.Vfs;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.io.IOException;

/**
 * User: ron
 * Date: Oct 9, 2009
 */
public class VfsTest {

    private void testVfsDir(URL url) {
        Assert.assertNotNull(url);

        Vfs.Dir dir = Vfs.fromURL(url);
        Assert.assertNotNull(dir);

        Iterable<Vfs.File> files = dir.getFiles();
        Vfs.File first = files.iterator().next();
        Assert.assertNotNull(first);

        first.getName();
        try {
            first.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        dir.close();
    }

    @Test
    public void vfsFromJar() {
        testVfsDir(getSomeJar());
    }

    @Test
    public void vfsFromDir() {
        testVfsDir(getSomeDirectory());
    }

    @Test
    public void vfsFromJarFileUrl() throws MalformedURLException {
        testVfsDir(new URL("jar:file://" + getSomeJar().getPath() + "!/"));
    }

    @Test
    public void vfsFromOtherDirType() {
        //todo http jar
    }

    @Test
    public void vfsFromJarWithInnerJars() {
        //todo?
    }

    @Test
    public void normalizeUrls() throws MalformedURLException {
        Assert.assertEquals("remove protocol", Vfs.normalizePath(new URL("file:/path/file.ext")), "/path/file.ext");
        Assert.assertEquals("remove protocol but leave windows drive character", Vfs.normalizePath(new URL("file:C:\\path\\file.ext")), "c:/path/file.ext");
        Assert.assertEquals("remove extra / at the end", Vfs.normalizePath(new URL("file:/path/file.ext/")), "/path/file.ext");
        Assert.assertEquals("remove multiple slashes", Vfs.normalizePath(new URL("file://path///file.ext//")), "/path/file.ext");
        Assert.assertEquals("remove jar url prefix and ! postfix", Vfs.normalizePath(new URL("jar:file:/path/file.jar!/something")), "/path/file.jar");
    }

    //
    public URL getSomeJar() {
        Collection<URL> urls = ClasspathHelper.getUrlsForCurrentClasspath();
        for (URL url : urls) {
            if (url.getFile().endsWith(".jar")) {
                return url;
            }
        }

        Assert.fail("could not find jar url");
        return null;
    }

    private URL getSomeDirectory() {
        try {
            return new URL("file:" + ReflectionsTest.getUserDir());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
