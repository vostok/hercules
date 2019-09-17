package ru.kontur.vostok.hercules.auth.wrapper;

import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HandlerWrapper;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;

/**
 * Wrap HttpHandler with authentication by ordinary api key.
 *
 * @author Vladimir Tsypaev
 */
public class OrdinaryAuthHandlerWrapper implements HandlerWrapper {
    private final AuthProvider authProvider;

    public OrdinaryAuthHandlerWrapper(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        return request -> {
            if (!authProvider.authenticateOrdinary(request)) {
                request.complete(HttpStatusCodes.UNAUTHORIZED);
            } else {
                handler.handle(request);
            }
        };
    }
}

