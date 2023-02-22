package ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Aleksandr Yuferov
 */

@SuppressWarnings("unused")
public class SentryEvent {

    private UUID eventId;

    private final ContextContainer contexts = new ContextContainer();

    private SdkVersion sdk;

    @Unsupported
    private Request request;

    private Map<String, String> tags = new HashMap<>();

    private String release;

    private String environment;

    private String platform;

    private User user;

    @Unsupported
    private String serverName;

    @Unsupported
    private String dist;

    @Unsupported
    private List<Breadcrumb> breadcrumbs;

    private List<String> fingerprint = new ArrayList<>();

    private String transaction;

    @Unsupported
    private ListWrapper<SentryThread> threads;

    @JsonProperty("exception")
    private ListWrapper<SentryException> exceptions;

    private Instant timestamp;

    private String logger;

    private Message message;

    private SentryLevel level;

    private Map<String, Object> extra = new HashMap<>();

    public List<SentryException> getExceptions() {
        if (exceptions == null) {
            return null;
        }
        return exceptions.getValues();
    }

    public void setExceptions(List<SentryException> exceptions) {
        this.exceptions = new ListWrapper<>(exceptions);
    }

    public UUID getEventId() {
        return this.eventId;
    }

    public ContextContainer getContexts() {
        return this.contexts;
    }

    public SdkVersion getSdk() {
        return this.sdk;
    }

    public Map<String, String> getTags() {
        return this.tags;
    }

    public String getRelease() {
        return this.release;
    }

    public String getEnvironment() {
        return this.environment;
    }

    public String getPlatform() {
        return this.platform;
    }

    public User getUser() {
        return this.user;
    }

    public List<String> getFingerprint() {
        return this.fingerprint;
    }

    public String getTransaction() {
        return this.transaction;
    }

    public Instant getTimestamp() {
        return this.timestamp;
    }

    public String getLogger() {
        return this.logger;
    }

    public Message getMessage() {
        return this.message;
    }

    public SentryLevel getLevel() {
        return this.level;
    }

    public Map<String, Object> getExtra() {
        return this.extra;
    }

    public void setEventId(UUID eventId) {
        this.eventId = eventId;
    }

    public void setSdk(SdkVersion sdk) {
        this.sdk = sdk;
    }

    public void setTags(Map<String, String> tags) {
        this.tags = tags;
    }

    public void setRelease(String release) {
        this.release = release;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    public void setFingerprint(List<String> fingerprint) {
        this.fingerprint = fingerprint;
    }

    public void setTransaction(String transaction) {
        this.transaction = transaction;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public void setLogger(String logger) {
        this.logger = logger;
    }

    public void setMessage(Message message) {
        this.message = message;
    }

    public void setLevel(SentryLevel sentryLevel) {
        this.level = sentryLevel;
    }

    public void setExtra(Map<String, Object> extra) {
        this.extra = extra;
    }

    public void putExtra(String key, Object value) {
        extra.put(key, value);
    }

    public void putTag(String key, String value) {
        tags.put(key, value);
    }

    public static class ListWrapper<T> {

        private final List<T> values;

        public ListWrapper(List<T> values) {
            this.values = values;
        }

        public List<T> getValues() {
            return this.values;
        }
    }
}
