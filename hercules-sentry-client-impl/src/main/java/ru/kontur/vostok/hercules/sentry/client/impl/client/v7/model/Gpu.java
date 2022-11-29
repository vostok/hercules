package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"unused"})
public class Gpu implements JsonUnknown {

    private String name;
    private Integer id;
    private Integer vendorId;
    private String vendorName;
    private Integer memorySize;
    private String apiType;
    private Boolean multiThreadedRendering;
    private String version;
    private String npotSupport;
    private Map<String, Object> unknown;

    public @Nullable Boolean isMultiThreadedRendering() {
        return multiThreadedRendering;
    }

    @Override
    public Gpu setUnknown(Map<String, Object> unknown) {
        this.unknown = unknown;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public Integer getId() {
        return this.id;
    }

    public Integer getVendorId() {
        return this.vendorId;
    }

    public String getVendorName() {
        return this.vendorName;
    }

    public Integer getMemorySize() {
        return this.memorySize;
    }

    public String getApiType() {
        return this.apiType;
    }

    public Boolean getMultiThreadedRendering() {
        return this.multiThreadedRendering;
    }

    public String getVersion() {
        return this.version;
    }

    public String getNpotSupport() {
        return this.npotSupport;
    }

    public Map<String, Object> getUnknown() {
        return this.unknown;
    }

    public static final class JsonKeys {
        public static final String NAME = "name";
        public static final String ID = "id";
        public static final String VENDOR_ID = "vendor_id";
        public static final String VENDOR_NAME = "vendor_name";
        public static final String MEMORY_SIZE = "memory_size";
        public static final String API_TYPE = "api_type";
        public static final String MULTI_THREADED_RENDERING = "multi_threaded_rendering";
        public static final String VERSION = "version";
        public static final String NPOT_SUPPORT = "npot_support";
    }
}
