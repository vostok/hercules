package ru.kontur.vostok.hercules.http;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * HTTP Response abstraction.
 *
 * @author Gregory Koshelev
 */
public interface HttpServerResponse {
    /**
     * Set HTTP status code.
     *
     * @param code the status code to set
     */
    void setStatusCode(int code);

    /**
     * Get HTTP status code.
     *
     * @return the status code of the response
     */
    int getStatusCode();

    /**
     * Set HTTP header to the response.
     *
     * @param header the header's name
     * @param value  the header's value
     */
    void setHeader(String header, String value);

    /**
     * Set HTTP header {@code Content-Type} to the response.
     *
     * @param contentType the content type
     */
    default void setContentType(String contentType) {
        setHeader(HttpHeaders.CONTENT_TYPE, contentType);
    }

    default void setContentLength(int length) {
        setHeader(HttpHeaders.CONTENT_LENGTH, String.valueOf(length));
    }

    /**
     * Send the data to the client using async IO. The Request is ended when done.
     * <p>
     * If it possible to send data in the same thread it will. Otherwise asynchronously send in another worker thread.
     *
     * @param data    the data to send
     * @param charset the encoding is used to send the data
     */
    default void send(String data, Charset charset) {
        send(ByteBuffer.wrap(data.getBytes(charset)));
    }

    /**
     * Send the data to the client using async IO. The Request is ended when done.
     * <p>
     * If it possible to send data in the same thread it will. Otherwise asynchronously send in another worker thread.
     * <p>
     * Use {@link StandardCharsets#UTF_8 UTF-8} encoding to send the data.
     *
     * @param data the data to send
     */
    default void send(String data) {
        send(data, StandardCharsets.UTF_8);
    }

    /**
     * Send buffers to the client using async IO.
     * Callback will be called when IO operations are completed.
     * Error callback will be called in case of IO exceptions.
     * <p>
     * If it possible to send data in the same thread it will. Otherwise asynchronously send in another worker thread.
     * <p>
     * The response header {@link HttpHeaders#CONTENT_LENGTH Content-Length} should be provided
     * if not used {@link HttpHeaders#TRANSFER_ENCODING Transfer-Encoding} "chunked".
     * <p>
     * The Request should be ended explicitly after either of callbacks was called.
     *
     * @param buffers       buffers to send
     * @param callback      the callback when IO operations are completed
     * @param errorCallback the error callback if IO exceptions are thrown
     */
    void send(ByteBuffer[] buffers, IoCallback callback, ErrorCallback errorCallback);

    /**
     * Send buffers to the client using async IO. The Request is ended when done.
     * <p>
     * If it possible to send data in the same thread it will. Otherwise asynchronously send in another worker thread.
     * <p>
     * The response header {@link HttpHeaders#CONTENT_LENGTH Content-Length} will be set up implicitly
     * if not used {@link HttpHeaders#TRANSFER_ENCODING Transfer-Encoding} "chunked".
     *
     * @param buffers buffers to send
     */
    void send(ByteBuffer[] buffers);

    /**
     * Send the buffer to the client using async IO.
     * Callback will be called when IO operations are completed.
     * Error callback will be called in case of IO exceptions.
     * <p>
     * If it possible to send data in the same thread it will. Otherwise asynchronously send in another worker thread.
     * <p>
     * The response header {@link HttpHeaders#CONTENT_LENGTH Content-Length} should be provided
     * if not used {@link HttpHeaders#TRANSFER_ENCODING Transfer-Encoding} "chunked".
     * <p>
     * The Request should be ended explicitly after either of callbacks was called.
     *
     * @param buffer        the buffer to send
     * @param callback      the callback when IO operations are completed
     * @param errorCallback the error callback if IO exceptions are thrown
     */
    default void send(ByteBuffer buffer, IoCallback callback, ErrorCallback errorCallback) {
        send(new ByteBuffer[]{buffer}, callback, errorCallback);
    }

    /**
     * Send the buffer to the client using async IO. The Request is ended when done.
     * <p>
     * If it possible to send data in the same thread it will. Otherwise asynchronously send in another worker thread.
     * <p>
     * The response header {@link HttpHeaders#CONTENT_LENGTH Content-Length} will be set up implicitly
     * if not used {@link HttpHeaders#TRANSFER_ENCODING Transfer-Encoding} "chunked".
     *
     * @param buffer the buffer to send
     */
    default void send(ByteBuffer buffer) {
        send(new ByteBuffer[]{buffer});
    }
}
