package ru.kontur.vostok.hercules.sentry.sink;

import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class ErrorInfo {

    private String message;
    private int code;
    private Boolean isRetryable;
    private long waitingTimeMs;

    public ErrorInfo(String message) {
        this.message = message;
    }

    public ErrorInfo(String message, int code) {
        this.message = message;
        this.code = code;
    }

    public ErrorInfo(boolean isRetryable) {
        this.isRetryable = isRetryable;
    }

    public ErrorInfo(boolean isRetryable, long waitingTimeMs) {
        this.isRetryable = isRetryable;
        this.waitingTimeMs = waitingTimeMs;
    }


    public ErrorInfo(int code) {
        this.code = code;
    }

    public ErrorInfo(int code, long waitingTimeMs) {
        this.code = code;
        this.waitingTimeMs = waitingTimeMs;
    }

    public long getWaitingTimeMs() {
        return waitingTimeMs;
    }

    public boolean isRetryable() {
        return isRetryable;
    }

    public void setIsRetryableForApiClient() throws BackendServiceFailedException {
        if (isRetryable != null) {
            return;
        }
        if (RETRYABLE_ERROR_CODES_FOR_API_CLIENT.contains(code) || code >= 500) {
            this.isRetryable = true;
        } else if (NON_RETRYABLE_ERROR_CODES_FOR_API_CLIENT.contains(code)) {
            this.isRetryable = false;
        } else {
            throw new BackendServiceFailedException();
        }
    }

    public void setIsRetryableForSending() throws BackendServiceFailedException {
        if (isRetryable != null) {
            return;
        }
        if (RETRYABLE_ERROR_CODES_FOR_SENDING.contains(code) || code >= 500) {
            this.isRetryable = true;
        } else if (NON_RETRYABLE_ERROR_CODES_FOR_SENDING.contains(code)) {
            this.isRetryable = false;
        } else {
            throw new BackendServiceFailedException();
        }
    }

    public boolean needToRemoveClientFromCache() {
        return ERROR_CODES_NEED_TO_UPDATE_CACHE.contains(code);
    }

    private static final Set<Integer> RETRYABLE_ERROR_CODES_FOR_API_CLIENT = new HashSet<>(Arrays.asList(
            404, 408, 409, 429
    ));

    private static final Set<Integer> NON_RETRYABLE_ERROR_CODES_FOR_API_CLIENT = new HashSet<>(Arrays.asList(
            400, 405, 413, 414, 415
    ));

    private static final Set<Integer> RETRYABLE_ERROR_CODES_FOR_SENDING = new HashSet<>(Arrays.asList(
            401, 403, 404, 408, 429
    ));

    private static final Set<Integer> NON_RETRYABLE_ERROR_CODES_FOR_SENDING = new HashSet<>(Arrays.asList(
            400, 405, 413, 414, 415
    ));

    private static final Set<Integer> ERROR_CODES_NEED_TO_UPDATE_CACHE = new HashSet<>(Arrays.asList(
            401, 403, 404
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
