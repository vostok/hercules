package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class SdkVersion {

    private String name;
    private String version;
    @Unsupported
    private List<SentryPackage> packages;
    @Unsupported
    private List<String> integrations;
    @Unsupported
    private Map<String, Object> unknown;

    public String getName() {
        return this.name;
    }

    public String getVersion() {
        return this.version;
    }

    public SdkVersion setName(String name) {
        this.name = name;
        return this;
    }

    public SdkVersion setVersion(String version) {
        this.version = version;
        return this;
    }
}
