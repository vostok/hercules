package ru.kontur.vostok.hercules.auth;

import ru.kontur.vostok.hercules.http.ContentTypes;
import ru.kontur.vostok.hercules.http.HttpServerRequest;
import ru.kontur.vostok.hercules.http.HttpStatusCodes;
import ru.kontur.vostok.hercules.http.header.HttpHeaders;

/**
 * @author Evgeniy Zatoloka
 */
public final class AuthUtil {

    /**
     * Extracts api key value from Authorization or apiKey header of http server request.
     *
     * @param request the http request.
     * @return api key or <code>null</code> if key is not present.
     */
    public static String getApiKey(HttpServerRequest request) {
        String apiKey = getAuthHeaderValueByPrefix(request, "Hercules apiKey ");
        if (apiKey == null) {
            apiKey = request.getHeader("apiKey");
        }
        return apiKey;
    }

    /**
     * Extracts master api key value from Authorization or masterApiKey header of http server request.
     *
     * @param request the http request.
     * @return master api key or <code>null</code> if key is not present in both headers.
     */
    public static String getMasterApiKey(HttpServerRequest request) {
        String masterApiKey = getAuthHeaderValueByPrefix(request, "Hercules masterApiKey ");
        if (masterApiKey == null) {
            masterApiKey = request.getHeader("masterApiKey");
        }
        return masterApiKey;
    }

    public static String getAuthHeaderValueByPrefix(HttpServerRequest request, String prefix) {
        String authValue = null;
        String authHeaderValue = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeaderValue != null) {
            if (authHeaderValue.startsWith(prefix)) {
                authValue = authHeaderValue.substring(prefix.length());
            }
        }
        return authValue;
    }

    /**
     * Try to complete the HTTP request with either {@code 401 Bad Request} status if api key is unknown
     * or {@code 403 Forbidden} status if api key doesn't have necessary rights.
     * <p>
     * An authResult error message is used to make a response message.
     *
     * @param request    the HTTP request
     * @param authResult the authorization result
     * @return {@code true} if request has been completed due to unsuccessful authorization, otherwise {@code false}
     */
    public static boolean tryCompleteRequestIfUnsuccessfulAuth(HttpServerRequest request, AuthResult authResult) {
        if (!authResult.isSuccess()) {
            if (authResult.isUnknown()) {
                request.complete(HttpStatusCodes.UNAUTHORIZED, ContentTypes.TEXT_PLAIN_UTF_8, authResult.getMessage());
                return true;
            }
            request.complete(HttpStatusCodes.FORBIDDEN, ContentTypes.TEXT_PLAIN_UTF_8, authResult.getMessage());
            return true;
        }
        return false;
    }

    private AuthUtil() {
        /* Static class */
    }
}
