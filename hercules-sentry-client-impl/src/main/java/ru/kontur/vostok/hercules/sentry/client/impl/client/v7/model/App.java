package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Map;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class App implements JsonUnknown {

    private String appIdentifier;
    private String appStartTime;
    private String deviceAppHash;
    private String buildType;
    private String appName;
    private String appVersion;
    private String appBuild;
    private Map<String, String> permissions;
    private Map<String, Object> unknown;

    @Override
    public App setUnknown(@Nullable Map<String,Object> unknown) {
        this.unknown = unknown;
        return this;
    }

    public String getAppIdentifier() {
        return this.appIdentifier;
    }

    public String getAppStartTime() {
        return this.appStartTime;
    }

    public String getDeviceAppHash() {
        return this.deviceAppHash;
    }

    public String getBuildType() {
        return this.buildType;
    }

    public String getAppName() {
        return this.appName;
    }

    public String getAppVersion() {
        return this.appVersion;
    }

    public String getAppBuild() {
        return this.appBuild;
    }

    public Map<String, String> getPermissions() {
        return this.permissions;
    }

    public Map<String, Object> getUnknown() {
        return this.unknown;
    }

    public static final class JsonKeys {
        public static final String APP_IDENTIFIER = "app_identifier";
        public static final String APP_START_TIME = "app_start_time";
        public static final String DEVICE_APP_HASH = "device_app_hash";
        public static final String BUILD_TYPE = "build_type";
        public static final String APP_NAME = "app_name";
        public static final String APP_VERSION = "app_version";
        public static final String APP_BUILD = "app_build";
        public static final String APP_PERMISSIONS = "permissions";
    }
}
