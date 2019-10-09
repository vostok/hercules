package ru.kontur.vostok.hercules.http.path;

import org.jetbrains.annotations.NotNull;

/**
 * Represents normalized request path.
 * <p>
 * Normalization consists of two steps:<br>
 * 1. Removing of leading and trailing slashes from the path<br>
 * 2. Splitting path into substrings by slashes
 * <p>
 * Normalization performs in lazy-fashion.
 *
 * @author Gregory Koshelev
 */
public class Path {
    private final String path;
    private volatile String[] normalizedPath;

    public static Path of(String path) {
        return new Path(path);
    }

    private Path(@NotNull String path) {
        this.path = path;
    }

    @NotNull
    public String getPath() {
        return path;
    }

    @NotNull
    public String[] getNormalizedPath() {
        if (normalizedPath == null) {
            normalizedPath = PathUtil.normalizePath(path);
        }
        return normalizedPath;
    }

    public int size() {
        return getNormalizedPath().length;
    }
}
