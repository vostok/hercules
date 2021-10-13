package ru.kontur.vostok.hercules.application;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author Gregory Koshelev
 */
public class ApplicationContext {
    private final String applicationName;
    private final String applicationId;
    private final String version;
    private final String commitId;
    private final String environment;
    private final String zone;
    private final String instanceId;
    private final String hostname;

    private final ConcurrentMap<String, String> additionalContext = new ConcurrentHashMap<>();

    public ApplicationContext(
            String applicationName,
            String applicationId,
            String version,
            String commitId,
            String environment,
            String zone,
            String instanceId,
            String hostname) {
        this.applicationName = applicationName;
        this.applicationId = applicationId;
        this.version = version;
        this.commitId = commitId;
        this.environment = environment;
        this.zone = zone;
        this.instanceId = instanceId;
        this.hostname = hostname;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getVersion() {
        return version;
    }

    public String getCommitId() {
        return commitId;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getZone() {
        return zone;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getHostname() {
        return hostname;
    }

    /**
     * Returns additional context value by the key.
     * These values can be exchanged thread-safely between different components.
     *
     * @param key the key whose associated value is to be returned
     * @return the value to which the specified key is mapped, or
     *         {@code null} if this map contains no mapping for the key
     */
    public String get(String key) {
        return additionalContext.get(key);
    }

    /**
     * Associates the specified additional context value with the specified key.
     *
     * @param key   key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     */
    public void put(String key, String value) {
        additionalContext.put(key, value);
    }
}
