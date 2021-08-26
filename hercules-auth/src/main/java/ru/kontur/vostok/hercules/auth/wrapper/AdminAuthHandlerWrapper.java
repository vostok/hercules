package ru.kontur.vostok.hercules.auth.wrapper;

import ru.kontur.vostok.hercules.auth.AuthProvider;
import ru.kontur.vostok.hercules.http.ContentTypes;
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
                request.complete(HttpStatusCodes.UNAUTHORIZED, ContentTypes.TEXT_PLAIN_UTF_8,
                        "Master api key is not valid or absent");
            } else {
                handler.handle(request);
            }
        };
    }
}
