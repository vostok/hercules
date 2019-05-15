package ru.kontur.vostok.hercules.undertow.util;

import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.throttling.RequestWeigher;

import java.util.Optional;

/**
 * @author Gregory Koshelev
 */
public class DefaultHttpServerRequestWeigher implements RequestWeigher<HttpServerRequest> {
    @Override
    public int weigh(HttpServerRequest request) {
        Optional<Integer> weigh = request.getContentLength();
        return weigh.orElse(-1);
    }
}
