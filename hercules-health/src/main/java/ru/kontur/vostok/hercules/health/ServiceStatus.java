package ru.kontur.vostok.hercules.health;

/**
 * ServiceStatus
 *
 * @author Kirill Sulim
 */
public enum ServiceStatus implements IHaveStatusCode {
    OK(0),
    SUSPENDED(1),
    STARTING(null),
    STOPPING(null)
    ;

    private final Integer statusCode;

    @Override
    public Integer getStatusCode() {
        return statusCode;
    }

    ServiceStatus(Integer statusCode) {
        this.statusCode = statusCode;
    }
}
