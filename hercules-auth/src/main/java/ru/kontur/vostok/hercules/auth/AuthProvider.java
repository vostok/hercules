package ru.kontur.vostok.hercules.auth;

import ru.kontur.vostok.hercules.health.Counter;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.util.text.StringUtil;

/**
 * Auth provider is used to authenticate http requests and authorize access to resources (streams or timelines).
 *
 * @author Gregory Koshelev
 */
public class AuthProvider {
    private static final String AUTH_CONTEXT = "authContext";

    private final AdminAuthManager adminAuthManager;
    private final AuthManager authManager;
    private final MetricsCollector metricsCollector;

    private final Counter apiKeyRequestCounter;
    private final Counter masterApiKeyRequestCounter;

    public AuthProvider(AdminAuthManager adminAuthManager, AuthManager authManager, MetricsCollector metricsCollector) {
        this.adminAuthManager = adminAuthManager;
        this.authManager = authManager;

        this.metricsCollector = metricsCollector;

        this.apiKeyRequestCounter = this.metricsCollector.counter("apiKeyRequestCount");
        this.masterApiKeyRequestCounter = this.metricsCollector.counter("masterApiKeyRequestCount");
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
        //FIXME: Replace with AuthUtil.getMasterApiKey(request) after migration to the Authorization header
        String masterApiKey = AuthUtil.getAuthHeaderValueByPrefix(request, "Hercules masterApiKey ");
        if (masterApiKey == null) {
            masterApiKey = request.getHeader("masterApiKey");
            if (!StringUtil.isNullOrEmpty(masterApiKey)) {
                masterApiKeyRequestCounter.increment();
            }
        }
        boolean hasAuthenticated = !StringUtil.isNullOrEmpty(masterApiKey) && adminAuthManager.auth(masterApiKey).isSuccess();
        request.putContext(AUTH_CONTEXT, hasAuthenticated ? AuthContext.master(masterApiKey) : AuthContext.notAuthenticated());
        return hasAuthenticated;
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
        //FIXME: Replace with AuthUtil.getApiKey(request) after migration to the Authorization header
        String apiKey = AuthUtil.getAuthHeaderValueByPrefix(request, "Hercules apiKey ");
        if (apiKey == null) {
            apiKey = request.getHeader("apiKey");
            if (!StringUtil.isNullOrEmpty(apiKey)) {
                apiKeyRequestCounter.increment();
            }
        }
        boolean hasAuthenticated = !StringUtil.isNullOrEmpty(apiKey) && authManager.hasApiKey(apiKey);
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

    /**
     * Authorize any access from {@code rights} using authentication context of the http request.
     * If at least one of right from {@code rights} is authorize success, then return {@link AuthResult#ok()}.
     *
     * @param request the http request
     * @param name    the resource name
     * @param rights  access rights
     * @return authorization result
     */
    public AuthResult authAny(HttpServerRequest request, String name, Right... rights) {
        if (rights.length == 0) {
            return AuthResult.unknown();
        }
        AuthResult result;
        for (Right right : rights) {
            if (right == Right.READ) {
                result = authRead(request, name);
                if (result.isSuccess()) {
                    return result;
                }
                continue;
            }
            if (right == Right.WRITE) {
                result = authWrite(request, name);
                if (result.isSuccess()) {
                    return result;
                }
                continue;
            }
            if (right == Right.MANAGE) {
                result = authManage(request, name);
                if (result.isSuccess()) {
                    return result;
                }
            }
        }
        return AuthResult.unknown();
    }
}
