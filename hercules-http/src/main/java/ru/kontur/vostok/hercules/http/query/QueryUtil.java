package ru.kontur.vostok.hercules.http.query;

import ru.kontur.vostok.hercules.http.ContentTypes;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
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
     * @param parameter the query parameter
     * @param request   the http request
     * @param <T>       the query parameter type
     * @return the query parameter value
     */
    public static <T> Parameter<T>.ParameterValue get(Parameter<T> parameter, HttpServerRequest request) {
        String requestParameter = request.getQueryParameter(parameter.name());
        return parameter.from(requestParameter);
    }

    /**
     * Complete the HTTP request with {@code 400 Bad Request} status due to validation error of the parameter value.
     * <p>
     * An validation error message is used to make a response message.
     *
     * @param request the HTTP request
     * @param value   the query parameter value
     * @param <T>     the query parameter type
     * @return {@code true} if request has been completed due to error, otherwise {@code false}
     */
    public static <T> boolean tryCompleteRequestIfError(HttpServerRequest request, Parameter<T>.ParameterValue value) {
        if (value.isError()) {
            request.complete(
                    HttpStatusCodes.BAD_REQUEST,
                    ContentTypes.TEXT_PLAIN_UTF_8,
                    makeErrorResponseMessage(value));
            return true;
        }
        return false;
    }

    /**
     * Make an error response message from validation error of {@link Parameter.ParameterValue}.
     *
     * @param value the query parameter value
     * @param <T>   the query parameter type
     * @return error response message
     */
    private static <T> String makeErrorResponseMessage(Parameter<T>.ParameterValue value) {
        return "Query parameter '" + value.parameter().name() + "' is invalid: " + value.result().error();
    }

    private QueryUtil() {
        /* static class */
    }
}
