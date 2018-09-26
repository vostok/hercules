package ru.kontur.vostok.hercules.health;

/**
 * ServiceStatus
 *
 * @author Kirill Sulim
 */
public enum ServiceStatus implements IHaveStatusCode {
    OK(0),
    SUSPENDED(1),
    ;

    private final int statusCode;

    @Override
    public int getStatusCode() {
        return statusCode;
    }

    ServiceStatus(int statusCode) {
        this.statusCode = statusCode;
    }
}
