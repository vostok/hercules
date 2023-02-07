package ru.kontur.vostok.hercules.sd;

/**
 * @author Gregory Koshelev
 */
public class BeaconInfo {
    private final String applicationName;
    private final String applicationId;
    private final String version;
    private final String commitId;
    private final String environment;
    private final String zone;
    private final String instanceId;
    private final String address;

    public BeaconInfo(
            String applicationName,
            String applicationId,
            String version,
            String commitId,
            String environment,
            String zone,
            String instanceId,
            String address) {
        this.applicationName = applicationName;
        this.applicationId = applicationId;
        this.version = version;
        this.commitId = commitId;
        this.environment = environment;
        this.zone = zone;
        this.instanceId = instanceId;
        this.address = address;
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

    public String getAddress() {
        return address;
    }
}
