package ru.kontur.vostok.hercules.util.application;

/**
 * ApplicationContext - stores information about running application such as name, instance identifier, environment etc.
 *
 * @author Kirill Sulim
 */
@Deprecated
public class ApplicationContext {

    /**
     * Human readable application name
     */
    private final String applicationName;

    /**
     * Robot readable application name
     */
    private final String applicationId;

    /**
     * Application version (semver preffered)
     */
    private final String version;

    /**
     * Commit id
     */
    private final String commitId;

    /**
     * Environment in which service is running (production, testing etc.)
     */
    private final String environment;

    /**
     * Datacenter in which instance is located
     */
    private final String zone;

    /**
     * Server host name
     */
    private final String hostName;

    /**
     * Instance identifier
     */
    private final String instanceId;


    public ApplicationContext(
            String applicationName,
            String applicationId,
            String version,
            String commitId,
            String environment,
            String zone,
            String hostName,
            String instanceId
    ) {
        this.applicationName = applicationName;
        this.applicationId = applicationId;
        this.version = version;
        this.commitId = commitId;
        this.environment = environment;
        this.zone = zone;
        this.hostName = hostName;
        this.instanceId = instanceId;
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

    public String getHostName() {
        return hostName;
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
}
