package ru.kontur.vostok.hercules.meta.sink.sentry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * SentryProjectMappingRecord record from sentry registry
 *
 * @author Kirill Sulim
 */
public class SentryProjectMappingRecord {

    private final String project;
    private final String service;
    private final String sentryOrganization;
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
