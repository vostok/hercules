package ru.kontur.vostok.hercules.meta.sink.sentry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SentryProjectMappingRecord record from sentry registry
 * A record stores correspondence of fields in Hercules and Sentry concepts
 *
 * @author Kirill Sulim
 */
public class SentryProjectMappingRecord {

    /**
     * A project in Hercules concept
     */
    private final String project;

    /**
     * A service in Hercules concept
     */
    private final String service;

    /**
     * A organisation in Sentry concept
     */
    private final String sentryOrganization;

    /**
     * A project in Sentry concept
     */
    private final String sentryProject;

    @JsonCreator
    public SentryProjectMappingRecord(
        @JsonProperty("project") @NotNull final String project,
        @JsonProperty("service") @Nullable final String service,
        @JsonProperty("sentryOrganization") @NotNull final String sentryOrganization,
        @JsonProperty("sentryProject") @NotNull final String sentryProject
    ) {
        this.project = project;
        this.service = service;
        this.sentryOrganization = sentryOrganization;
        this.sentryProject = sentryProject;
    }

    public String getProject() {
        return project;
    }

    public String getService() {
        return service;
    }

    public String getSentryProject() {
        return sentryProject;
    }

    public String getSentryOrganization() {
        return sentryOrganization;
    }
}
