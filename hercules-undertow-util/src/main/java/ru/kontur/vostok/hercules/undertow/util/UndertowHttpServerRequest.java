package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.http.ErrorCallback;
import ru.kontur.vostok.hercules.http.HttpMethod;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpServerRequestException;
import ru.kontur.vostok.hercules.http.HttpServerResponse;
import ru.kontur.vostok.hercules.http.NotSupportedHttpMethodException;
import ru.kontur.vostok.hercules.http.ReadBodyCallback;
import ru.kontur.vostok.hercules.http.RequestCompletionListener;
import ru.kontur.vostok.hercules.util.collection.CollectionUtil;

/**
 * @author Gregory Koshelev
 */
public class UndertowHttpServerRequest implements HttpServerRequest {
    private final HttpServerExchange exchange;
    private final HttpServerResponse response;
    private volatile HttpMethod method;

    public UndertowHttpServerRequest(HttpServerExchange exchange) {
        this.exchange = exchange;
        this.response = new UndertowHttpServerResponse(exchange);
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
    public void dispatchAsync(Runnable runnable) {
        exchange.dispatch(runnable);
    }

    @Override
    public void readBodyAsync(ReadBodyCallback callback, ErrorCallback errorCallback) {
        try {
            exchange.getRequestReceiver().receiveFullBytes(
                    (exchange, bytes) -> callback.dispatch(this, bytes),
                    (exchange, exception) -> errorCallback.error(this, new HttpServerRequestException(exception)));
        } catch (Throwable throwable) {
            errorCallback.error(this, new HttpServerRequestException(throwable));
        }
    }

    @Override
    public HttpServerResponse getResponse() {
        return response;
    }

    @Override
    public void complete() {
        exchange.endExchange();
    }

    @Override
    public void addRequestCompletionListener(RequestCompletionListener listener) {
        exchange.addExchangeCompleteListener((exch, nextListener) -> {
            try {
                listener.onComplete(this);
            } finally {
                nextListener.proceed();
            }
        });
    }
}
