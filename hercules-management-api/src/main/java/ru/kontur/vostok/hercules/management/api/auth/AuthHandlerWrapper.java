package ru.kontur.vostok.hercules.management.api.auth;

import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HandlerWrapper;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;

/**
 * Authenticate by master api key or ordinal api key
 *
 * @author Gregory Koshelev
 */
public class AuthHandlerWrapper implements HandlerWrapper {
    private final AuthProvider authProvider;

    public AuthHandlerWrapper(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        return request -> {
            if (!authProvider.authenticate(request)) {
                request.complete(HttpStatusCodes.UNAUTHORIZED);
            } else {
                handler.handle(request);
            }
        };
    }
}
