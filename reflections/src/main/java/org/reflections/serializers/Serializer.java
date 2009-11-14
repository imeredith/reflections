package org.reflections.serializers;

import org.reflections.Reflections;

import java.io.File;
import java.io.InputStream;

/**
 * Created by IntelliJ IDEA.
 * User: ron
 * Date: Oct 28, 2009
 */
public interface Serializer {
    /** reads the input stream into a new Reflections instance, populating it's store */
    Reflections read(InputStream inputStream);

    /** saves a Reflections instance into the given filename */
    File save(Reflections reflections, String filename);

    /** returns a string serialization of the given Reflections instance */
    String toString(Reflections reflections);
}