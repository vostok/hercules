package ru.kontur.vostok.hercules.http;

/**
 * @author Gregory Koshelev
 */
public interface HttpServerResponse {
    void setStatusCode(int code);
    void setHeader(String header, String value);
}
