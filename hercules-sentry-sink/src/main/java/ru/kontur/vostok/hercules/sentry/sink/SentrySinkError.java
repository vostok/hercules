package ru.kontur.vostok.hercules.sentry.sink;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SentrySinkError {

    private String message;
    private int code;
    private boolean isRetryable;
    private long waitingTimeMs;

    public SentrySinkError(String message) {
        this.message = message;
    }

    public SentrySinkError(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public SentrySinkError(String message, boolean isRetryable) {
        this.message = message;
        this.isRetryable = isRetryable;
    }

    public SentrySinkError(Boolean isRetryable) {
        this.isRetryable = isRetryable;
    }

    public SentrySinkError(Boolean isRetryable, long waitingTimeMs) {
        this.isRetryable = isRetryable;
        this.waitingTimeMs = waitingTimeMs;
    }


    public SentrySinkError(int code) {
        this.code = code;
        setIsRetryableByCode();
    }

    public SentrySinkError(int code, long waitingTimeMs) {
        this.code = code;
        this.waitingTimeMs = waitingTimeMs;
        setIsRetryableByCode();
    }

    public long getWaitingTimeMs() {
        return waitingTimeMs;
    }

    public boolean isRetryable() {
        return isRetryable;
    }

    public boolean needToUpdate() {
        return NEED_TO_UPDATE_ERRORS_CODES.contains(code);
    }

    private void setIsRetryableByCode() {
        if (RETRYABLE_ERRORS_CODES.contains(code)) {
            this.isRetryable = true;
        }
    }

    private static final Set<Integer> RETRYABLE_ERRORS_CODES = new HashSet<>(Arrays.asList(
            401, 403, 404, 408, 409, 429, 500, 503
    ));

    private static final Set<Integer> NEED_TO_UPDATE_ERRORS_CODES = new HashSet<>(Arrays.asList(
            401, 403, 404, 409
    ));

    @Override
    public String toString() {
        String string = "";
        if (code != 0) {
            string += code + " ";
        }
        if (message != null) {
            string += message;
        }
        return string;
    }
}
