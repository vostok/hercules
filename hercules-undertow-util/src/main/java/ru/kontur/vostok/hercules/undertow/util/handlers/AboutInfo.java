package ru.kontur.vostok.hercules.undertow.util.handlers;

/**
 * AboutInfo
 *
 * @author Kirill Sulim
 */
public class AboutInfo {

    private final String serviceName;
    private final String version;
    private final String commitHash;
    private final String environment;
    private final String instanceId;
    private final String hostName;

    public AboutInfo(String serviceName, String version, String commitHash, String environment, String instanceId, String hostName) {
        this.serviceName = serviceName;
        this.version = version;
        this.commitHash = commitHash;
        this.environment = environment;
        this.instanceId = instanceId;
        this.hostName = hostName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public String getVersion() {
        return version;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getEnvironment() {
        return environment;
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getHostName() {
        return hostName;
    }
}
