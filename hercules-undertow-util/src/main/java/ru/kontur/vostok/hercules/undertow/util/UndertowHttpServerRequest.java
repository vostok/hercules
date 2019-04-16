package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.http.HttpMethod;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpServerRequestException;
import ru.kontur.vostok.hercules.http.HttpServerResponse;
import ru.kontur.vostok.hercules.http.NotSupportedHttpMethodException;
import ru.kontur.vostok.hercules.util.collection.CollectionUtil;

import java.util.Collections;

/**
 * @author Gregory Koshelev
 */
public class UndertowHttpServerRequest implements HttpServerRequest {
    private final HttpServerExchange exchange;
    private volatile HttpMethod method;
    private volatile HttpServerResponse response;

    public UndertowHttpServerRequest(HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public HttpMethod getMethod() throws NotSupportedHttpMethodException {
        return method != null ? method : (method = HttpMethodUtil.of(exchange.getRequestMethod()));
    }

    @Override
    public String getPath() {
        return exchange.getRequestPath();
    }

    @Override
    public String getHeader(String name) {
        return exchange.getRequestHeaders().getFirst(name);
    }

    @Override
    public String getParameter(String name) {
        return exchange.getQueryParameters().getOrDefault(name, CollectionUtil.emptyDeque()).peek();
    }

    @Override
    public String[] getParameterValues(String name) {
        return exchange.getQueryParameters().getOrDefault(name, CollectionUtil.emptyDeque()).toArray(new String[0]);
    }

    @Override
    public byte[] readBody() throws HttpServerRequestException {
        return new byte[0];
    }

    @Override
    public HttpServerResponse getResponse() {
        return response != null ? response : (response = new UndertowHttpServerResponse(exchange));
    }

    @Override
    public void complete() {
        exchange.endExchange();
    }
}
