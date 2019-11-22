package ru.kontur.vostok.hercules.client.test.util;

import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.StatusLine;
import org.apache.http.message.BasicStatusLine;
import ru.kontur.vostok.hercules.protocol.encoder.Encoder;
import ru.kontur.vostok.hercules.protocol.encoder.Writer;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * TestUtil
 *
 * @author Kirill Sulim
 */
public final class TestUtil {

    public static final StatusLine _200_OK = new BasicStatusLine(HttpVersion.HTTP_1_1, HttpStatus.SC_OK, "");

    public static <T> byte[] toBytes(T value, Writer<T> writer, int size) {
        ByteBuffer buffer = ByteBuffer.allocate(size);
        Encoder encoder = new Encoder(buffer);
        writer.write(encoder, value);
        return buffer.array();
    }

    private TestUtil() {
        /* static class */
    }
}
