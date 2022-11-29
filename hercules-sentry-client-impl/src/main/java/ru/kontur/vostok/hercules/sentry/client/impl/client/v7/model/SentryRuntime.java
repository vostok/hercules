package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Map;

@SuppressWarnings("unused")
public class SentryRuntime implements JsonUnknown {

    private String name;
    private String version;
    private String rawDescription;
    private Map<String, Object> unknown;
    @Override
    public SentryRuntime setUnknown(Map<String,Object> unknown) {
        this.unknown = unknown;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public String getRawDescription() {
        return this.rawDescription;
    }

    public Map<String, Object> getUnknown() {
        return this.unknown;
    }

    public static final class JsonKeys {
        public static final String NAME = "name";
        public static final String VERSION = "version";
        public static final String RAW_DESCRIPTION = "raw_description";
    }
}
