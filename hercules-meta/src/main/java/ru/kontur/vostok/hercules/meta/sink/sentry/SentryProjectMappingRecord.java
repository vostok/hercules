package ru.kontur.vostok.hercules.meta.sink.sentry;

/**
 * SentryProjectMappingRecord record from sentry registry
 *
 * @author Kirill Sulim
 */
public class SentryProjectMappingRecord {

    private String project;
    private String sentryProject;

    public SentryProjectMappingRecord() {
    }

    public SentryProjectMappingRecord(String project, String sentryProject) {
        this.project = project;
        this.sentryProject = sentryProject;
    }

    public String getProject() {
        return project;
    }

    public void setProject(String project) {
        this.project = project;
    }

    public String getSentryProject() {
        return sentryProject;
    }

    public void setSentryProject(String sentryProject) {
        this.sentryProject = sentryProject;
    }
}
