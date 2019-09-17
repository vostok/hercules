package ru.kontur.vostok.hercules.elastic.sink;

/**
 * @author a.zhdanov
 */
class ErrorInfo {
    private final ErrorType type;
    private final String reason;

    ErrorInfo(ErrorType type, String reason) {
        this.type = type;
        this.reason = reason;
    }

    ErrorType getType() {
        return type;
    }

    String getReason() {
        return reason;
    }

}
