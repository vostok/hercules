package ru.kontur.vostok.hercules.http;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Set of predefined values for the Content-Type header
 *
 * @author Gregory Koshelev
 */
public final class ContentTypes {
    /**
     * Content-Type {@code "text/plain; charset=UTF-8"}
     */
    public static final String TEXT_PLAIN_UTF_8 = withCharset(MimeTypes.TEXT_PLAIN, StandardCharsets.UTF_8);

    /**
     * Add the parameter to the Content-Type header value.
     * <p>
     * See <a href="https://tools.ietf.org/html/rfc2045">RFC 2045</a> for details.
     *
     * @param contentType the Content-Type header value
     * @param attribute   the parameter attribute
     * @param value       the parameter value
     * @return the Content-Type header value with the parameter
     */
    public static String withParameter(String contentType, String attribute, String value) {
        return contentType + "; " + attribute + "=" + value;
    }

    /**
     * Specify charset in the Content-Type header.
     *
     * @param contentType the Content-Type header value
     * @param charset     the charset
     * @return the Content-Type header value with a charset information
     */
    public static String withCharset(String contentType, Charset charset) {
        return withParameter(contentType, "charset", charset.name());
    }

    private ContentTypes() {
        /* static class */
    }
}
