package ru.kontur.vostok.hercules.undertow.util.authorization;

import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.handler.HandlerWrapper;
import ru.kontur.vostok.hercules.http.handler.HttpHandler;

/**
 * AdminAuthManagerWrapper
 *
 * @author Kirill Sulim
 */
public class AdminAuthManagerWrapper implements HandlerWrapper {

    private final AdminAuthManager adminAuthManager;

    public AdminAuthManagerWrapper(AdminAuthManager adminAuthManager) {
        this.adminAuthManager = adminAuthManager;
    }

    @Override
    public HttpHandler wrap(HttpHandler handler) {
        return request -> {
            String masterApiKey = request.getHeader("masterApiKey");
            if (masterApiKey == null || !adminAuthManager.auth(masterApiKey).isSuccess()) {
                request.complete(HttpStatusCodes.UNAUTHORIZED);
            } else {
                handler.handle(request);
            }
        };
    }
}
