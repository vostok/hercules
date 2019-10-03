package ru.kontur.vostok.hercules.http.path;

import org.jetbrains.annotations.NotNull;

/**
 * @author Gregory Koshelev
 */
public final class PathUtil {
    /**
     * Normalize URL path by splitting with slash.
     *
     * @param path the URL path
     * @return normalized path
     */
    @NotNull
    public static String[] normalizePath(@NotNull String path) {
        return path.split("/");
    }

    private PathUtil() {
        /* static class */
    }
}
