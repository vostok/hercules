package ru.kontur.vostok.hercules.client.test.util;

import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.Writer;

import java.io.ByteArrayOutputStream;

/**
 * TestUtil
 *
 * @author Kirill Sulim
 */
public final class TestUtil {

    public static <T> byte[] toBytes(T value, Writer<T> writer) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        Encoder encoder = new Encoder(stream);
        writer.write(encoder, value);
        return stream.toByteArray();
    }

    private TestUtil() {
        /* static class */
    }
}
