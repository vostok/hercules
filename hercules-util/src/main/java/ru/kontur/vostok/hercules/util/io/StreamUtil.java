package ru.kontur.vostok.hercules.util.io;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static ru.kontur.vostok.hercules.util.throwable.ThrowableUtil.toUnchecked;

public final class StreamUtil {

    // Based on https://stackoverflow.com/a/35446009/2535153
    public static String toString(InputStream inputStream) {
        return toUnchecked(() -> {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8.name());
        });
    }

    public static InputStream toInputStream(String data) {
        return new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));
    }
}
