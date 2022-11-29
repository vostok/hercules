package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import java.util.Map;

@SuppressWarnings("unused")
public class User implements JsonUnknown {

    private String email;
    private String id;
    private String ipAddress;
    private String username;
    @Unsupported
    private Map<String, String> other;
    private Map<String, Object> unknown;

    @Override
    public User setUnknown(Map<String, Object> unknown) {
        this.unknown = unknown;
        return this;
    }

    public String getEmail() {
        return this.email;
    }

    public String getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }

    public Map<String, Object> getUnknown() {
        return this.unknown;
    }

    public static final class JsonKeys {
        public static final String EMAIL = "email";
        public static final String ID = "id";
        public static final String USERNAME = "username";
        public static final String IP_ADDRESS = "ip_address";
    }
}
