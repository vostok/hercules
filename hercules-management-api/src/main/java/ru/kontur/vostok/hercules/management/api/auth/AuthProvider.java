package ru.kontur.vostok.hercules.management.api.auth;

import ru.kontur.vostok.hercules.auth.AdminAuthManager;
import ru.kontur.vostok.hercules.auth.AuthManager;
import ru.kontur.vostok.hercules.auth.AuthResult;
import ru.kontur.vostok.hercules.http.HttpServerRequest;

/**
 * Auth provider is used to authenticate http requests and authorize access to resources (streams or timelines).
 *
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

    /**
     * Authenticate by the master api key.
     * <p>
     * Set {@link AuthContext#master(String)} in auth context of {@link HttpServerRequest} if succeeded
     * or {@link AuthContext#notAuthenticated()} otherwise.
     *
     * @param request the http request
     * @return {@code true} if authenticate successfully, or {@code false} otherwise
     */
    public boolean authenticateMaster(HttpServerRequest request) {
        String masterApiKey = request.getHeader("masterApiKey");
        AuthResult authResult = adminAuthManager.auth(masterApiKey);
        request.putContext(AUTH_CONTEXT, authResult.isSuccess() ? AuthContext.master(masterApiKey) : AuthContext.notAuthenticated());
        return authResult.isSuccess();
    }

    /**
     * Authenticate by the ordinary api key.
     * <p>
     * Set {@link AuthContext#ordinary(String)} in auth context of {@link HttpServerRequest} if succeeded
     * or {@link AuthContext#notAuthenticated()} otherwise.
     *
     * @param request the http request
     * @return {@code true} if authenticate successfully, or {@code false} otherwise
     */
    public boolean authenticateOrdinary(HttpServerRequest request) {
        String apiKey = request.getHeader("apiKey");
        boolean hasAuthenticated = authManager.hasApiKey(apiKey);
        request.putContext(AUTH_CONTEXT, hasAuthenticated ? AuthContext.ordinary(apiKey) : AuthContext.notAuthenticated());
        return hasAuthenticated;
    }

    /**
     * Authenticate by the master or the ordinary api key.
     * <p>
     * Would try to authenticate by the ordinary api key only if do not authenticate successfully by the master api key.
     *
     * @param request the http request
     * @return {@code true} if authenticate successfully by any of choices,
     * or {@code false} if do not authenticate by neither the master nor the ordinary api keys
     */
    public boolean authenticate(HttpServerRequest request) {
        if (authenticateMaster(request)) {
            return true;
        }
        if (authenticateOrdinary(request)) {
            return true;
        }
        return false;
    }

    /**
     * Authorize manage access using authentication context of the http request.
     *
     * @param request the http request
     * @param name    the resource name
     * @return authorization result
     */
    public AuthResult authManage(HttpServerRequest request, String name) {
        AuthContext context = request.getContext(AUTH_CONTEXT);
        if (context == null) {
            return AuthResult.unknown();
        }
        switch (context.getAuthenticationType()) {
            case MASTER:
                return AuthResult.ok();
            case ORDINARY:
                return authManager.authManage(context.getApiKey(), name);
            default:
                return AuthResult.unknown();
        }
    }

    /**
     * Authorize read access using authentication context of the http request.
     *
     * @param request the http request
     * @param name    the resource name
     * @return authorization result
     */
    public AuthResult authRead(HttpServerRequest request, String name) {
        AuthContext context = request.getContext(AUTH_CONTEXT);
        if (context == null) {
            return AuthResult.unknown();
        }
        switch (context.getAuthenticationType()) {
            case MASTER:
                return AuthResult.ok();
            case ORDINARY:
                return authManager.authRead(context.getApiKey(), name);
            default:
                return AuthResult.unknown();
        }
    }

    /**
     * Authorize write access using authentication context of the http request.
     *
     * @param request the http request
     * @param name    the resource name
     * @return authorization result
     */
    public AuthResult authWrite(HttpServerRequest request, String name) {
        AuthContext context = request.getContext(AUTH_CONTEXT);
        if (context == null) {
            return AuthResult.unknown();
        }
        switch (context.getAuthenticationType()) {
            case MASTER:
                return AuthResult.ok();
            case ORDINARY:
                return authManager.authWrite(context.getApiKey(), name);
            default:
                return AuthResult.unknown();
        }
    }
}
