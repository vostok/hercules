package ru.kontur.vostok.hercules.util.application;

/**
 * ApplicationContext - stores information about running application such as name, instance identifier, environment etc.
 *
 * @author Kirill Sulim
 */
public class ApplicationContext {

    private final String hostName;

    private final String name;
    private final String environment;
    private final String instanceId;

    private final String version;
    private final String commitHash;


    public ApplicationContext(
            String hostName,
            String name,
            String environment,
            String instanceId,
            String version,
            String commitHash
    ) {
        this.hostName = hostName;
        this.name = name;
        this.environment = environment;
        this.instanceId = instanceId;
        this.version = version;
        this.commitHash = commitHash;
    }

    public String getHostName() {
        return hostName;
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

    public String getVersion() {
        return version;
    }

    public String getCommitHash() {
        return commitHash;
    }
}
