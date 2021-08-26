package ru.kontur.vostok.hercules.auth.wrapper;

import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.http.ContentTypes;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HandlerWrapper;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;

/**
 * Authenticate by master api key or ordinary api key
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
                request.complete(HttpStatusCodes.UNAUTHORIZED, ContentTypes.TEXT_PLAIN_UTF_8,
                        "Both master api key and api key are either not valid or absent");
            } else {
                handler.handle(request);
            }
        };
    }
}
