package ru.kontur.vostok.hercules.management.api.auth;

/**
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

    public static AuthContext master(String masterApiKey) {
        return new AuthContext(masterApiKey, null, AuthenticationType.MASTER);
    }

    public static AuthContext ordinal(String apiKey) {
        return new AuthContext(null, apiKey, AuthenticationType.ORDINAL);
    }

    public static AuthContext notAuthenticated() {
        return new AuthContext(null, null, AuthenticationType.NOT_AUTHENTICATED);
    }
}
