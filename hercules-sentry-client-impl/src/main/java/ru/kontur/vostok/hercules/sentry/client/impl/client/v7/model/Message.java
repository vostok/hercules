package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class Message {

    @Unsupported
    private String formatted;
    private String message;
    @Unsupported
    private List<String> params;
    @Unsupported
    private Map<String, Object> unknown;

    public String getMessage() {
        return this.message;
    }

    public Message setMessage(String message) {
        this.message = message;
        return this;
    }
}
