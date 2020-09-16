package ru.kontur.vostok.hercules.elastic.sink;

/**
 * @author a.zhdanov
 */
class ErrorInfo {
    private final ErrorType type;
    private final String error;

    ErrorInfo(ErrorType type, String error) {
        this.type = type;
        this.error = error;
    }

    ErrorType getType() {
        return type;
    }

    String getError() {
        return error;
    }

}
