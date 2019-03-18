package ru.kontur.vostok.hercules.undertow.util.authorization;

import io.undertow.server.HandlerWrapper;
import io.undertow.server.HttpHandler;
import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.undertow.util.ExchangeUtil;
import ru.kontur.vostok.hercules.undertow.util.ResponseUtil;

import java.util.Optional;

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
        return exchange -> {
            Optional<String> apiKey = ExchangeUtil.extractHeaderValue(exchange, "apiKey");
            if (!apiKey.isPresent() || !adminAuthManager.auth(apiKey.get()).isSuccess()) {
                ResponseUtil.unauthorized(exchange);
            } else {
                handler.handleRequest(exchange);
            }
        };
    }
}
