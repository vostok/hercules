package ru.kontur.vostok.hercules.sentry.client.impl.exceptions;

public class SentryApiException extends Exception {
    private int code;
    public SentryApiException(String formatString, String ... params) {
        super(String.format(formatString, (Object[]) params));
    }
    public void setCode(int code) {
        this.code = code;
    }
    public int getCode() {
        return code;
    }
}
