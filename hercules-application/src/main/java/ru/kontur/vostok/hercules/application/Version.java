package ru.kontur.vostok.hercules.application;

import ru.kontur.vostok.hercules.configuration.PropertiesLoader;

import java.util.Properties;

/**
 * Represents application version
 *
 * @author Gregory Koshelev
 */
public final class Version {

    private static final Version INSTANCE;

    static {
        Properties properties = PropertiesLoader.load("resource://git.properties", false);

        final String version = properties.getProperty("git.build.version", "unknown");
        final String commitId = properties.getProperty("git.commit.id", "unknown");

        INSTANCE = new Version(version, commitId);
    }

    /**
     * Version. Semantic versioning is used.
     * <p>
     * Version has {@code <major>.<minor>.<patch>[-<modifier>]} format.<br>
     * If version cannot be determined, then version would equal 'unknown'.
     */
    private final String version;
    /**
     * Commit id hash.
     * <p>
     * If commit id cannot be determined, then it would equal 'unknown'.
     */
    private final String commitId;

    private Version(String version, String commitId) {
        this.version = version;
        this.commitId = commitId;
    }

    public String getVersion() {
        return version;
    }

    public String getCommitId() {
        return commitId;
    }

    /**
     * Return application version. Singleton object is returned.
     *
     * @return application version
     */
    public static Version get() {
        return INSTANCE;
    }
}
