package ru.kontur.vostok.hercules.http;

import java.util.Map;
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
     * @param name header name
     * @return header value or <code>null</code> if it doesn't exist
     */
    String getHeader(String name);

    /**
     * Get header values of the request. If there are no header values in the request, then return empty array.
     *
     * @param name header name
     * @return header values
     */
    String[] getHeaders(String name);

    /**
     * Get query parameter value of the request. If parameter is used for multiple times, then return first value.
     *
     * @param name query parameter name
     * @return parameter value or <code>null</code> if it doesn't exist
     */
    String getQueryParameter(String name);

    /**
     * Get path parameter value of the request.
     * <p>
     * Sample:<br>
     * If registered path template is {@code "/path/:book/:page"} and
     * requested url is {@code "/path/thehitchhikersguidetothegalaxy/42"}
     * then<br>
     * getPathParameter("book") returns "thehitchhikersguidetothegalaxy" and
     * getPathParameter("page") returns "42"
     *
     * @param name path parameter name
     * @return parameter value or <code>null</code> if it doesn't exist
     */
    String getPathParameter(String name);

    /**
     * Set path parameters.
     *
     * @param pathParameters path parameters
     */
    void setPathParameters(Map<String, String> pathParameters);

    /**
     * Get all query parameter values of the request. If parameter doesn't present, then return empty array.
     *
     * @param name query parameter name
     * @return parameter values
     */
    String[] getQueryParameterValues(String name);

    /**
     * Asynchronously dispatch HTTP request.
     */
    void dispatchAsync(Runnable runnable);

    /**
     * Asynchronously read request's body to byte array.
     * <p>
     * Can be called only once.
     *
     * @param callback      the callback is called to process request's body
     * @param errorCallback the callback is called in case of errors
     */
    void readBodyAsync(ReadBodyCallback callback, ErrorCallback errorCallback);

    /**
     * Asynchronously read request's body to byte array.
     * <p>
     * Can be called only once.
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
     * Complete the request processing.
     * <p>
     * This method is no-op if the request is already completed.
     */
    void complete();

    /**
     * Complete request processing with specified status code.
     *
     * @param code response status code
     */
    default void complete(int code) {
        getResponse().setStatusCode(code);
        complete();
    }

    /**
     * Complete request processing with specified code and content in response body.
     * <p>
     * Completion may perform asynchronously.
     *
     * @param code        response status code
     * @param contentType content type
     * @param data        data to send in response body
     */
    default void complete(int code, String contentType, String data) {
        getResponse().setStatusCode(code);
        getResponse().setContentType(contentType);
        getResponse().send(data);
    }

    /**
     * Return {@link HttpHeaders#CONTENT_LENGTH Content-Length} value if present.
     *
     * @return content length if present, otherwise {@link Optional#empty()}
     */
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

    /**
     * Add the request completion listener to perform request post processing.
     *
     * @param listener the listener
     */
    void addRequestCompletionListener(RequestCompletionListener listener);

    /**
     * Put object to request context.
     * <p>
     * Context is thread-safe.
     * <p>
     * Also, it can be used to exchange authentication context between handlers in the handlers chain.
     *
     * @param key the key of the object
     * @param obj the object to put into context
     * @param <T> type
     */
    <T> void putContext(String key, T obj);

    /**
     * Get object from request context.
     * <p>
     * Context is thread-safe.
     *
     * @param key the key of the object
     * @param <T> type
     * @return the object from context or {@code null} if absent
     */
    <T> T getContext(String key);
}
