package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.throttling.RequestWeigher;

/**
 * @author Gregory Koshelev
 */
public class DefaultUndertowRequestWeigher implements RequestWeigher<HttpServerExchange> {
    @Override
    public int weigh(HttpServerExchange request) {
        return ExchangeUtil.extractContentLength(request);
    }
}
