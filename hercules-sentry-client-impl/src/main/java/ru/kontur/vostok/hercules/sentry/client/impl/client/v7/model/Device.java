package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"unused"})
public class Device implements JsonUnknown {

    private String name;
    private String manufacturer;
    private String brand;
    private String family;
    private String model;
    private String modelId;
    private List<String> archs;
    private Float batteryLevel;
    private Boolean charging;
    private Boolean online;
    private DeviceOrientation orientation;
    private Boolean simulator;
    private Long memorySize;
    private Long freeMemory;
    private Long usableMemory;
    private Boolean lowMemory;
    private Long storageSize;
    private Long freeStorage;
    private Long externalStorageSize;
    private Long externalFreeStorage;
    private Integer screenWidthPixels;
    private Integer screenHeightPixels;
    private Float screenDensity;
    private Integer screenDpi;
    private Date bootTime;
    private TimeZone timezone;
    private String id;
    @Deprecated
    private String language;
    private String locale;
    private String connectionType;
    private Float batteryTemperature;
    private Map<String, Object> unknown;

    public enum DeviceOrientation {
        PORTRAIT,
        LANDSCAPE
    }

    public Device() {}
    public String getName() {
        return name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public String getBrand() {
        return brand;
    }

    public String getFamily() {
        return family;
    }

    public String getModel() {
        return model;
    }

    public String getModelId() {
        return modelId;
    }

    public List<String> getArchs() {
        return archs;
    }

    public Float getBatteryLevel() {
        return batteryLevel;
    }

    public @Nullable Boolean isCharging() {
        return charging;
    }

    public @Nullable Boolean isOnline() {
        return online;
    }

    public DeviceOrientation getOrientation() {
        return orientation;
    }

    public @Nullable Boolean isSimulator() {
        return simulator;
    }

    public Long getMemorySize() {
        return memorySize;
    }

    public Long getFreeMemory() {
        return freeMemory;
    }

    public Long getUsableMemory() {
        return usableMemory;
    }

    public Boolean isLowMemory() {
        return lowMemory;
    }

    public Long getStorageSize() {
        return storageSize;
    }

    public Long getFreeStorage() {
        return freeStorage;
    }

    public Long getExternalStorageSize() {
        return externalStorageSize;
    }

    public Long getExternalFreeStorage() {
        return externalFreeStorage;
    }

    public Integer getScreenWidthPixels() {
        return screenWidthPixels;
    }

    public Integer getScreenHeightPixels() {
        return screenHeightPixels;
    }

    public Float getScreenDensity() {
        return screenDensity;
    }

    public Integer getScreenDpi() {
        return screenDpi;
    }

    public Date getBootTime() {
        return bootTime;
    }

    public TimeZone getTimezone() {
        return timezone;
    }

    public String getId() {
        return id;
    }

    public String getLanguage() {
        return language;
    }

    public String getLocale() {
        return locale;
    }

    public String getConnectionType() {
        return connectionType;
    }

    public Float getBatteryTemperature() {
        return batteryTemperature;
    }

    @Nullable
    @Override
    public Map<String, Object> getUnknown() {
        return unknown;
    }

    @Override
    public Device setUnknown(@Nullable Map<String,Object> unknown) {
        this.unknown = unknown;
        return this;
    }

    public static final class JsonKeys {
        public static final String NAME = "name";
        public static final String MANUFACTURER = "manufacturer";
        public static final String BRAND = "brand";
        public static final String FAMILY = "family";
        public static final String MODEL = "model";
        public static final String MODEL_ID = "model_id";
        public static final String ARCHS = "archs";
        public static final String BATTERY_LEVEL = "battery_level";
        public static final String CHARGING = "charging";
        public static final String ONLINE = "online";
        public static final String ORIENTATION = "orientation";
        public static final String SIMULATOR = "simulator";
        public static final String MEMORY_SIZE = "memory_size";
        public static final String FREE_MEMORY = "free_memory";
        public static final String USABLE_MEMORY = "usable_memory";
        public static final String LOW_MEMORY = "low_memory";
        public static final String STORAGE_SIZE = "storage_size";
        public static final String FREE_STORAGE = "free_storage";
        public static final String EXTERNAL_STORAGE_SIZE = "external_storage_size";
        public static final String EXTERNAL_FREE_STORAGE = "external_free_storage";
        public static final String SCREEN_WIDTH_PIXELS = "screen_width_pixels";
        public static final String SCREEN_HEIGHT_PIXELS = "screen_height_pixels";
        public static final String SCREEN_DENSITY = "screen_density";
        public static final String SCREEN_DPI = "screen_dpi";
        public static final String BOOT_TIME = "boot_time";
        public static final String TIMEZONE = "timezone";
        public static final String ID = "id";
        public static final String LANGUAGE = "language";
        public static final String CONNECTION_TYPE = "connection_type";
        public static final String BATTERY_TEMPERATURE = "battery_temperature";
        public static final String LOCALE = "locale";
    }
}
