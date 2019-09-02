package ru.kontur.vostok.hercules.management.api.auth;

import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.http.HttpServerRequest;

/**
 * @author Gregory Koshelev
 */
public class AuthProvider {
    private static final String AUTH_CONTEXT = "authContext";

    private final AdminAuthManager adminAuthManager;
    private final AuthManager authManager;

    public AuthProvider(AdminAuthManager adminAuthManager, AuthManager authManager) {
        this.adminAuthManager = adminAuthManager;
        this.authManager = authManager;
    }

    public boolean authenticateMaster(HttpServerRequest request) {
        String masterApiKey = request.getHeader("masterApiKey");
        AuthResult authResult = adminAuthManager.auth(masterApiKey);
        request.putContext(AUTH_CONTEXT, authResult.isSuccess() ? AuthContext.master(masterApiKey) : AuthContext.notAuthenticated());
        return authResult.isSuccess();
    }

    public boolean authenticateOrdinal(HttpServerRequest request) {
        String apiKey = request.getHeader("apiKey");
        boolean hasAuthenticated = authManager.hasApiKey(apiKey);
        request.putContext(AUTH_CONTEXT, hasAuthenticated ? AuthContext.ordinal(apiKey) : AuthContext.notAuthenticated());
        return hasAuthenticated;
    }

    public boolean authenticate(HttpServerRequest request) {
        if (authenticateMaster(request)) {
            return true;
        }
        if (authenticateOrdinal(request)) {
            return true;
        }
        return false;
    }

    public AuthResult authManage(HttpServerRequest request, String name) {
        AuthContext context = request.getContext(AUTH_CONTEXT);
        if (context == null) {
            return AuthResult.unknown();
        }
        switch (context.getAuthenticationType()) {
            case MASTER:
                return AuthResult.ok();
            case ORDINAL:
                return authManager.authManage(context.getApiKey(), name);
            default:
                return AuthResult.unknown();
        }
    }

    public AuthResult authRead(HttpServerRequest request, String name) {
        AuthContext context = request.getContext(AUTH_CONTEXT);
        if (context == null) {
            return AuthResult.unknown();
        }
        switch (context.getAuthenticationType()) {
            case MASTER:
                return AuthResult.ok();
            case ORDINAL:
                return authManager.authRead(context.getApiKey(), name);
            default:
                return AuthResult.unknown();
        }
    }

    public AuthResult authWrite(HttpServerRequest request, String name) {
        AuthContext context = request.getContext(AUTH_CONTEXT);
        if (context == null) {
            return AuthResult.unknown();
        }
        switch (context.getAuthenticationType()) {
            case MASTER:
                return AuthResult.ok();
            case ORDINAL:
                return authManager.authWrite(context.getApiKey(), name);
            default:
                return AuthResult.unknown();
        }
    }
}
