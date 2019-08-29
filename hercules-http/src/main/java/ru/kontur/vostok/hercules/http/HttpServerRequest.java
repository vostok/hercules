package ru.kontur.vostok.hercules.http;

import java.util.Optional;

/**
 * HTTP Request abstraction.
 *
 * @author Gregory Koshelev
 */
public interface HttpServerRequest {
    /**
     * Get HTTP-method of the request
     *
     * @return enum value <code>HttpMethod</code>
     */
    HttpMethod getMethod() throws NotSupportedHttpMethodException;

    /**
     * Get URL part without host:port and query parameters. Started with '/' character.
     *
     * @return path of the request
     */
    String getPath();

    /**
     * Get header value of the request. If header is used for multiple times, then return first value.
     *
     * @param name of the header
     * @return header value or <code>null</code> if it doesn't exist
     */
    String getHeader(String name);

    /**
     * Get query parameter value of the request. If parameter is used for multiple times, then return first value.
     *
     * @param name of the query parameter
     * @return parameter value or <code>null</code> if it doesn't exist
     */
    String getParameter(String name);

    /**
     * Get all query parameter values of the request. If parameter doesn't present, then return empty array.
     *
     * @param name of the query parameter
     * @return parameter values
     */
    String[] getParameterValues(String name);

    /**
     * Asynchronously dispatch HTTP request.
     */
    void dispatchAsync(Runnable runnable);

    /**
     * Asynchronously read request's body to byte array.
     *
     * @param callback      the callback is called to process request's body
     * @param errorCallback the callback is called in case of errors
     */
    void readBodyAsync(ReadBodyCallback callback, ErrorCallback errorCallback);

    /**
     * Asynchronously read request's body to byte array.
     *
     * @param callback the callback is called to process request's body
     */
    default void readBodyAsync(ReadBodyCallback callback) {
        readBodyAsync(
                callback,
                (request, exception) -> {
                    request.complete(HttpStatusCodes.INTERNAL_SERVER_ERROR);
                });
    }

    /**
     * Get corresponding response.
     *
     * @return response
     */
    HttpServerResponse getResponse();

    /**
     * Complete request processing. Can be called once.
     */
    void complete();

    /**
     * Complete request processing with specified status code.
     *
     * @param code of response
     */
    default void complete(int code) {
        getResponse().setStatusCode(code);
        complete();
    }

    /**
     * Complete request processing with specified code and content in response body.
     *
     * @param code response status code
     * @param contentType content type
     * @param data data to send in response body
     */
    default  void complete(int code, String contentType, String data) {
        getResponse().setStatusCode(code);
        getResponse().setContentType(contentType);
        getResponse().send(data);
    }

    default Optional<Integer> getContentLength() {
        String headerValue = getHeader(HttpHeaders.CONTENT_LENGTH);

        if (headerValue == null || headerValue.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.valueOf(headerValue));
        } catch (NumberFormatException ex) {
            return Optional.of(-1);
        }

    }

    void addRequestCompletionListener(RequestCompletionListener listener);
}
