package ru.kontur.vostok.hercules.util.application;

/**
 * Context
 *
 * @author Kirill Sulim
 */
public class Context {

    private final String applicationName;
    private final String environment;
    private final String instanceId;

    public Context(String applicationName, String environment, String instanceId) {
        this.applicationName = applicationName;
        this.environment = environment;
        this.instanceId = instanceId;
    }

    public String getApplicationName() {
        return applicationName;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
