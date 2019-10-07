package ru.kontur.vostok.hercules.http.path;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.util.text.StringUtil;

/**
 * @author Gregory Koshelev
 */
public final class PathUtil {
    /**
     * Normalize URL path by splitting by slash {@code '/'}.
     * <p>
     * Leading and trailing slashes are ignored.
     *
     * @param path the URL path
     * @return normalized path
     */
    @NotNull
    public static String[] normalizePath(@NotNull String path) {
        return StringUtil.split(path, '/');
    }

    private PathUtil() {
        /* static class */
    }
}
