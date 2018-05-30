package ru.kontur.vostok.hercules.auth;

/**
 * @author Gregory Koshelev
 */
public class AuthResult {
    private final AuthStatus status;
    private final String message;

    private AuthResult(AuthStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public boolean isSuccess() {
        return status == AuthStatus.SUCCESS;
    }

    public boolean isUnknown() {
        return status == AuthStatus.UNKNOWN;
    }

    public String getMessage() {
        return message;
    }

    /**
     * Successful auth result
     * @return successful result which is singleton (no new instances are created)
     */
    public static AuthResult ok() {
        return OK;
    }

    /**
     * Unsuccessful auth result when provided apiKey is unknown
     * @return unsuccessful result which is singleton (no new instances are created)
     */
    public static AuthResult unknown() {
        return UNKNOWN;
    }

    /**
     * Unsuccessful auth result when provided apiKey doesn't have enough access rights
     * @return unsuccessful result which is singleton (no new instances are created)
     */
    public static AuthResult denied() {
        return DENIED;
    }

    /**
     * Unsuccessful auth result when some error has been acquired
     * @param message of error
     * @return unsuccessful result (new instance is created)
     */
    public static AuthResult error(String message) {
        return new AuthResult(AuthStatus.ERROR, message);
    }

    private static AuthResult OK = new AuthResult(AuthStatus.SUCCESS, "");
    private static AuthResult UNKNOWN = new AuthResult(AuthStatus.UNKNOWN, "Unknown apiKey");
    private static AuthResult DENIED = new AuthResult(AuthStatus.DENIED, "Access is denied (check if apiKey has appropriate access rights)");

    private enum AuthStatus {
        SUCCESS,
        UNKNOWN,
        DENIED,
        ERROR
    }
}
