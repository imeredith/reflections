package org.reflections.vfs;

import com.google.common.collect.AbstractIterator;

import java.io.File;
import java.util.zip.ZipEntry;
import java.util.Iterator;
import java.util.Enumeration;
import java.net.URL;
import java.io.IOException;

/** an implementation of {@link org.reflections.vfs.Vfs.Dir} for {@link java.util.zip.ZipFile} */
public class ZipDir implements Vfs.Dir {
    final java.util.zip.ZipFile zipFile;
    private String path;

    public ZipDir(URL url) {
        this(Vfs.normalizePath(url));
    }

    public ZipDir(String path) {
        this.path = path;
        java.util.zip.ZipFile result;
        try {
            result = new java.util.zip.ZipFile(path);
        }
        catch (IOException e) {throw new RuntimeException(e);}
        zipFile = result;
    }

    public String getPath() {
        return path;
    }

    public Iterable<Vfs.File> getFiles() {
        return new Iterable<Vfs.File>() {
            public Iterator<Vfs.File> iterator() {
                return new AbstractIterator<Vfs.File>() {
                    final Enumeration<? extends ZipEntry> entries = zipFile.entries();

                    protected Vfs.File computeNext() {
                        return entries.hasMoreElements() ? new ZipFile(ZipDir.this, entries.nextElement()) : endOfData();
                    }
                };
            }
        };
    }

    public void close() {
        if (zipFile != null) {
            try {zipFile.close();}
            catch (IOException e) {throw new RuntimeException("could not close zip file " + path, e);}
        }
    }

    @Override
    public String toString() {
        return zipFile.getName();
    }
}
