package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class OperatingSystem implements JsonUnknown {

    private String name;
    private String version;
    private String rawDescription;
    private String build;
    private String kernelVersion;
    private Boolean rooted;
    private Map<String, Object> unknown;

    public @Nullable Boolean isRooted() {
        return rooted;
    }

    @Override
    public OperatingSystem setUnknown(Map<String,Object> unknown) {
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

    public String getBuild() {
        return this.build;
    }

    public String getKernelVersion() {
        return this.kernelVersion;
    }

    public Map<String, Object> getUnknown() {
        return this.unknown;
    }

    public static final class JsonKeys {
        public static final String NAME = "name";
        public static final String VERSION = "version";
        public static final String RAW_DESCRIPTION = "raw_description";
        public static final String BUILD = "build";
        public static final String KERNEL_VERSION = "kernel_version";
        public static final String ROOTED = "rooted";
    }
}
