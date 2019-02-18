package ru.kontur.vostok.hercules.elastic.sink;

import java.io.IOException;
import java.io.OutputStream;

/**
 * @author Gregory Koshelev
 */
public class ElasticUtil {
    public static void writeNewLine(OutputStream stream) throws IOException {
        stream.write('\n');
    }
}
