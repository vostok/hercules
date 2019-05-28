package ru.kontur.vostok.hercules.http;

import ru.kontur.vostok.hercules.util.bytes.SizeUnit;

/**
 * @author Gregory Koshelev
 */
public class HttpServerDefaults {
    public static final long DEFAULT_MAX_CONTENT_LENGTH = SizeUnit.MEGABYTES.toBytes(8);
    public static final int DEFAULT_CONNECTION_THRESHOLD = 100_000;

    private HttpServerDefaults() {
        /* static class */
    }
}
