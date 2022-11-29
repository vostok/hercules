package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Map;

@SuppressWarnings("unused")
public class SentryException {

    private String type;
    private String value;
    private String module;
    @Unsupported
    private Long threadId;
    private SentryStackTrace stacktrace;
    @Unsupported
    private Mechanism mechanism;
    @Unsupported
    private Map<String, Object> unknown;

    public String getType() {
        return this.type;
    }

    public String getValue() {
        return this.value;
    }

    public String getModule() {
        return this.module;
    }

    public SentryStackTrace getStacktrace() {
        return this.stacktrace;
    }

    public SentryException setType(String type) {
        this.type = type;
        return this;
    }

    public SentryException setValue(String value) {
        this.value = value;
        return this;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public void setStacktrace(SentryStackTrace stacktrace) {
        this.stacktrace = stacktrace;
    }
}
