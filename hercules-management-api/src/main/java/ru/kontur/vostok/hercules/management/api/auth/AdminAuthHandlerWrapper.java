package ru.kontur.vostok.hercules.management.api.auth;

import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HandlerWrapper;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;

/**
 * Wrap HttpHandler with authentication by master api key.
 *
 * @author Gregory Koshelev
 */
public class AdminAuthHandlerWrapper implements HandlerWrapper {
    private final AuthProvider authProvider;

    public AdminAuthHandlerWrapper(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        return request -> {
            if (!authProvider.authenticateMaster(request)) {
                request.complete(HttpStatusCodes.UNAUTHORIZED);
            } else {
                handler.handle(request);
            }
        };
    }
}
