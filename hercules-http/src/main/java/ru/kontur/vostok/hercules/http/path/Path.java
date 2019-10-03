package ru.kontur.vostok.hercules.http.path;

import org.jetbrains.annotations.NotNull;

/**
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
