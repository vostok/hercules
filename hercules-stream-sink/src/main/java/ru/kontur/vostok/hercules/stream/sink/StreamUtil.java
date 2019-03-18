package ru.kontur.vostok.hercules.stream.sink;

import ru.kontur.vostok.hercules.meta.stream.DerivedStream;

/**
 * @author Gregory Koshelev
 */
public class StreamUtil {
    public static String streamToApplicationId(DerivedStream stream) {
        return "hercules.sink.stream." + stream.getName();
    }
}
