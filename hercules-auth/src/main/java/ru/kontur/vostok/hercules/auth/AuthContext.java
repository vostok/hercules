package ru.kontur.vostok.hercules.auth;

/**
 * Authentication context.
 * <p>
 * {@link AuthContext} is used to provide authentication context between http handlers and {@link AuthProvider},
 *
 * @author Gregory Koshelev
 */
public class AuthContext {
    private final String masterApiKey;
    private final String apiKey;
    private final AuthenticationType authenticationType;

    private AuthContext(String masterApiKey, String apiKey, AuthenticationType authenticationType) {
        this.masterApiKey = masterApiKey;
        this.apiKey = apiKey;
        this.authenticationType = authenticationType;
    }

    public String getMasterApiKey() {
        return masterApiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public AuthenticationType getAuthenticationType() {
        return authenticationType;
    }

    /**
     * Authenticated by the master api key.
     *
     * @param masterApiKey the master api key
     * @return auth context has authenticated master api key
     */
    public static AuthContext master(String masterApiKey) {
        return new AuthContext(masterApiKey, null, AuthenticationType.MASTER);
    }

    /**
     * Authenticated by ordinary api key.
     *
     * @param apiKey the api key
     * @return auth context has authenticated ordinary api key
     */
    public static AuthContext ordinary(String apiKey) {
        return new AuthContext(null, apiKey, AuthenticationType.ORDINARY);
    }

    /**
     * Not authenticated.
     *
     * @return auth context has no authentication
     */
    public static AuthContext notAuthenticated() {
        return new AuthContext(null, null, AuthenticationType.NOT_AUTHENTICATED);
    }
}
