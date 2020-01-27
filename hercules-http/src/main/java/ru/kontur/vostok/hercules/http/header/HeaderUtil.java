package ru.kontur.vostok.hercules.http.header;

import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.util.parameter.Parameter;
import ru.kontur.vostok.hercules.util.parameter.ParameterValue;

/**
 * Http request header util
 *
 * @author Gregory Koshelev
 */
public class HeaderUtil {
    /**
     * Extracts the request header value.
     *
     * @param parameter the parameter
     * @param request   the http request
     * @param <T>       the value type
     * @return the request header value
     */
    public static <T> ParameterValue<T> get(Parameter<T> parameter, HttpServerRequest request) {
        String requestHeader = request.getHeader(parameter.name());
        return parameter.from(requestHeader);
    }
}
