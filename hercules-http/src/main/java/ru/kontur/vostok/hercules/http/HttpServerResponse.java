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
     * Send the data to the client. The Request is ended when done.
     *
     * @param data    the data to send
     * @param charset the encoding is used to send the data
     */
    void send(String data, Charset charset);

    /**
     * Send the data to the client. The Request is ended when done.
     * <p>
     * Use UTF-8 encoding to send the data.
     *
     * @param data the data to send
     */
    default void send(String data) {
        send(data, StandardCharsets.UTF_8);
    }

    /**
     * Send the buffer to the client. The Request is ended when done.
     *
     * @param buffer the buffer to send
     */
    void send(ByteBuffer buffer);
}
