package ru.kontur.vostok.hercules.http.header;

/**
 * @author Gregory Koshelev
 */
public final class HttpHeaders {
    public static final String ACCEPT_ENCODING = "Accept-Encoding";
    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";

    /* Non-Standard HTTP headers */

    /**
     * Non-Standard HTTP header contains original (non-compressed) content length
     * when {@link #CONTENT_ENCODING Content-Encoding} is used.
     * <p>
     * Thus, {@link #CONTENT_LENGTH Content-Length} refers to compressed content length,
     * but {@link #ORIGINAL_CONTENT_LENGTH} refers to uncompressed content length.
     */
    public static final String ORIGINAL_CONTENT_LENGTH = "Original-Content-Length";

    private HttpHeaders() {
        /* static class */
    }
}
