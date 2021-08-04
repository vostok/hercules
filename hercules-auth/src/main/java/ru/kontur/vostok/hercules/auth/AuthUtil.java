package ru.kontur.vostok.hercules.auth;

import ru.kontur.vostok.hercules.http.HttpServerRequest;
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

    private AuthUtil() {
        /* Static class */
    }
}
