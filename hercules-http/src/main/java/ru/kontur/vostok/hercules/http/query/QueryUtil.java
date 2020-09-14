package ru.kontur.vostok.hercules.http.query;

import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.util.parameter.Parameter;

/**
 * Http request query util
 *
 * @author Gregory Koshelev
 */
public final class QueryUtil {
    /**
     * Extracts the value of query parameter.
     *
     * @param parameter the parameter
     * @param request   the http request
     * @param <T>       the value type of parameter
     * @return the value of query parameter
     */
    public static <T> Parameter<T>.ParameterValue get(Parameter<T> parameter, HttpServerRequest request) {
        String requestParameter = request.getQueryParameter(parameter.name());
        return parameter.from(requestParameter);
    }

    private QueryUtil() {
        /* static class */
    }
}
