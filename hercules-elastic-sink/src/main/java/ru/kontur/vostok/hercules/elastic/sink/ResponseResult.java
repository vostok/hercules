package ru.kontur.vostok.hercules.elastic.sink;

/**
 * @author Gregory Koshelev
 */
public class ResponseResult {
    private static final ResponseResult NONE = new ResponseResult(null, null);

    private final ErrorType type;
    private final String error;

    private ResponseResult(ErrorType type, String error) {
        this.type = type;
        this.error = error;
    }

    public boolean isOk() {
        return this == NONE;
    }

    public boolean isError() {
        return this != NONE;
    }
}
