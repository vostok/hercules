package ru.kontur.vostok.hercules.undertow.util;

import io.undertow.server.HttpServerExchange;
import ru.kontur.vostok.hercules.throttling.RequestWeigher;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class DefaultUndertowRequestWeigher implements RequestWeigher<HttpServerExchange> {
    @Override
    public int weigh(HttpServerExchange request) {
        Optional<Integer> weigh = ExchangeUtil.extractContentLength(request);
        return weigh.orElse(-1);
    }
}
