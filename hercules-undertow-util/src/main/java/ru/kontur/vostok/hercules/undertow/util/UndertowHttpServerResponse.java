package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import ru.kontur.vostok.hercules.http.HttpServerResponse;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * @author Gregory Koshelev
 */
public class UndertowHttpServerResponse implements HttpServerResponse {
    private final HttpServerExchange exchange;

    public UndertowHttpServerResponse(HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public void setStatusCode(int code) {
        exchange.setStatusCode(code);
    }

    @Override
    public int getStatusCode() {
        return exchange.getStatusCode();
    }

    @Override
    public void setHeader(String header, String value) {
        exchange.getResponseHeaders().put(HttpString.tryFromString(header), value);
    }

    @Override
    public void send(String data, Charset charset) {
        exchange.getResponseSender().send(data, charset);
    }

    @Override
    public void send(ByteBuffer buffer) {
        exchange.getResponseSender().send(buffer);
    }
}
