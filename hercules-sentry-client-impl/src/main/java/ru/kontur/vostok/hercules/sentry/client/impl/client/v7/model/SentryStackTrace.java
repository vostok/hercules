package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class SentryStackTrace {

    private List<SentryStackFrame> frames;
    @Unsupported
    private Map<String, String> registers;
    @Unsupported
    private Boolean snapshot;
    @Unsupported
    private Map<String, Object> unknown;

    public List<SentryStackFrame> getFrames() {
        return this.frames;
    }

    public SentryStackTrace setFrames(List<SentryStackFrame> frames) {
        this.frames = frames;
        return this;
    }

}
