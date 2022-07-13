package ru.kontur.vostok.hercules.http.path;

import org.jetbrains.annotations.NotNull;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
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

    /**
     * Extracts the value of path parameter.
     *
     * @param parameter the path parameter
     * @param request   the http request
     * @param <T>       the path parameter type
     * @return the path parameter value
     */
    public static <T> Parameter<T>.ParameterValue get(Parameter<T> parameter, HttpServerRequest request) {
        String pathParameter = request.getPathParameter(parameter.name());
        return parameter.from(pathParameter);
    }

    private PathUtil() {
        /* static class */
    }
}
