package ru.kontur.vostok.hercules.http;

/**
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
     * Get complete request body as byte array. Can be called once since content is read as stream.
     *
     * @return request body byte array
     */
    byte[] readBody() throws HttpServerRequestException;

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
}
