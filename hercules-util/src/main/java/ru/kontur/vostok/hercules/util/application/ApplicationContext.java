package ru.kontur.vostok.hercules.util.application;

/**
 * ApplicationContext - stores information about running application such as name, instance identifier, environment etc.
 *
 * @author Kirill Sulim
 */
public class ApplicationContext {

    private final String name;
    private final String environment;
    private final String instanceId;

    public ApplicationContext(String name, String environment, String instanceId) {
        this.name = name;
        this.environment = environment;
        this.instanceId = instanceId;
    }

    public String getName() {
        return name;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getInstanceId() {
        return instanceId;
    }
}
