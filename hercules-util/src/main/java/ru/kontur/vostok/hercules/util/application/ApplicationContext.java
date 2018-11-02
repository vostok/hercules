package ru.kontur.vostok.hercules.util.application;

/**
 * ApplicationContext - stores information about running application such as name, instance identifier, environment etc.
 *
 * @author Kirill Sulim
 */
public class ApplicationContext {

    private final String hostName;

    private final String name;
    private final String id;
    private final String environment;
    private final String instanceId;

    private final String version;
    private final String commitId;


    public ApplicationContext(
            String hostName,
            String name,
            String id,
            String environment,
            String instanceId,
            String version,
            String commitId
    ) {
        this.hostName = hostName;
        this.name = name;
        this.id = id;
        this.environment = environment;
        this.instanceId = instanceId;
        this.version = version;
        this.commitId = commitId;
    }

    public String getHostName() {
        return hostName;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
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

    public String getCommitId() {
        return commitId;
    }
}
