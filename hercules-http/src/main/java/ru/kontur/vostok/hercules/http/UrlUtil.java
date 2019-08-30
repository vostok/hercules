package ru.kontur.vostok.hercules.http;

/**
 * URL utility class
 *
 * @author Gregory Koshelev
 */
public final class UrlUtil {
    /**
     * Join two valid url path strings together with slash.<br>
     * Valid url path is slash-separated and url-encoded.
     *
     * @param prefix the path prefix (ex. root path)
     * @param path   the path
     * @return composed path
     */
    public static String join(String prefix, String path) {
        if (prefix.endsWith("/") && path.startsWith("/")) {
            return prefix + path.substring(1);
        }

        if (prefix.endsWith("/") || path.startsWith("/")) {
            return prefix + path;
        }

        return prefix + "/" + path;
    }

    private UrlUtil() {
        /* static class */
    }
}
