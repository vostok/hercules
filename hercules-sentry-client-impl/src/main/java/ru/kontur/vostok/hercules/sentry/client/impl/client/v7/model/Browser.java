package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class Browser implements JsonUnknown {

    private String name;
    private String version;
    private Map<String, Object> unknown;

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    @Nullable
    @Override
    public Map<String, Object> getUnknown() {
        return unknown;
    }

    @Override
    public Browser setUnknown(Map<String,Object> unknown) {
        this.unknown = unknown;
        return this;
    }

    public static final class JsonKeys {
        public static final String NAME = "name";
        public static final String VERSION = "version";
    }
}
