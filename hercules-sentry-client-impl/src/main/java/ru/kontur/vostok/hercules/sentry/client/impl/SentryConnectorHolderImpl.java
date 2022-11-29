package ru.kontur.vostok.hercules.sentry.client.impl;

import com.google.common.cache.CacheLoader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.sentry.client.ErrorInfo;
import ru.kontur.vostok.hercules.sentry.client.SentryConnectorHolder;
import ru.kontur.vostok.hercules.sentry.client.api.SentryApiClient;
import ru.kontur.vostok.hercules.sentry.client.api.model.OrganizationInfo;
import ru.kontur.vostok.hercules.sentry.client.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.sentry.client.api.model.TeamInfo;
import ru.kontur.vostok.hercules.sentry.client.impl.client.cache.DsnCache;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.DsnFetcherClient;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Dsn;
import ru.kontur.vostok.hercules.sentry.client.impl.exceptions.SentryApiException;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;

public class SentryConnectorHolderImpl implements SentryConnectorHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(SentryConnectorHolder.class);
    private final DsnCache vault;
    private final SentryApiClient sentryApiClient;
    private final DsnFetcherClient dsnFetcherClient;

    public SentryConnectorHolderImpl(
            SentryApiClient sentryApiClient,
            long cacheTtlMs,
            DsnFetcherClient dsnFetcherClient
    ) {
        this.sentryApiClient = sentryApiClient;
        this.dsnFetcherClient = dsnFetcherClient;
        Function<SentryDestination, Dsn> createConnector = destination -> {
            Result<List<OrganizationInfo>, ErrorInfo> organizationsResult = sentryApiClient.getOrganizations();
            if (!organizationsResult.isOk()) {
                LOGGER.error("Unable to search organizations in Sentry: "
                        + organizationsResult.getError());
                throw new RuntimeException(organizationsResult.getError().toString());
            }
            boolean orgExists = false;
            for (OrganizationInfo organizationInfo : organizationsResult.get()) {
                if (organizationInfo.getSlug().equals(destination.organization())) {
                    orgExists = true;
                    break;
                }
            }
            if (!orgExists) {
                LOGGER.error("Organization '{}' not found in Sentry: {}",
                        destination.organization(), organizationsResult.getError());
                return Dsn.unavailable();
            }
            Dsn dsn;
            try {
                dsn = createProjectIfNotExists(destination);
            } catch (SentryApiException e) {
                throw new RuntimeException(e.getMessage());
            }
            return dsn;
        };
        this.vault = new DsnCache(cacheTtlMs, createConnector);
        this.dsnFetcherClient.setVault(vault);
    }

    public Result<Dsn, ErrorInfo> getOrCreateConnector(SentryDestination destination) {
        try {
             return Result.ok(vault.cacheAndGet(destination));
        } catch (ExecutionException e) {
            return Result.error(new ErrorInfo(e.getCause().getMessage()));
        } catch (CacheLoader.InvalidCacheLoadException e) {
            return Result.ok(null);
        }

    }

    private Dsn createProjectIfNotExists(SentryDestination destination) throws
            SentryApiException {
        String organization = destination.organization();
        String project = destination.project();
        Result<List<ProjectInfo>, ErrorInfo> projectsResult = sentryApiClient.getProjects(
                organization);
        if (!projectsResult.isOk()) {
            LOGGER.error("Cannot get projects of organization '{}': {}", organization,
                    projectsResult.getError());
            throw new SentryApiException("Cannot get projects of organization %s: %s",
                    organization,
                    projectsResult.getError().getType());
        }
        boolean projExists = false;
        for (ProjectInfo projectInfo : projectsResult.get()) {
            if (projectInfo.getSlug().equals(project)) {
                projExists = true;
                break;
            }
        }
        if (!projExists) {
            String team = createDefaultTeamIfNotExists(organization);
            Result<ProjectInfo, ErrorInfo> creationResult = sentryApiClient.createProject(
                    organization, team, project);
            if (!creationResult.isOk()) {
                LOGGER.warn("Cannot create project '{}' in organization '{}': {}", project,
                        organization,
                        creationResult.getError());
                throw new SentryApiException("Cannot create project %s in organization %s: %s",
                        organization,
                        project,
                        creationResult.getError().getType());
            }
            LOGGER.info("Project '{}' is created in organization '{}'", project, organization);
        }
        try {
            Dsn dsn = dsnFetcherClient.fetchDsn(destination);
            LOGGER.info("Project '{}' in organization  '{}' is uploaded into cache", project,
                    organization);
            return dsn;
        } catch (Exception e) {
            LOGGER.error("Cannot create dsn for project '{}' in organization '{}'", project,
                    organization);
            throw new SentryApiException("Cannot create dsn for project %s in organization %s: %s",
                    organization,
                    project,
                    e.getMessage());
        }
    }

    /**
     * Check default team existence in the organization in the Sentry.
     * If default team does not exist the method create new team
     *
     * @param organization the organization
     * @return the {@link Result} object with team name.
     */
    private String createDefaultTeamIfNotExists(String organization) throws SentryApiException {
        Result<List<TeamInfo>, ErrorInfo> teamsResult = sentryApiClient.getTeams(organization);
        if (!teamsResult.isOk()) {
            LOGGER.error("Cannot get teams of organization '{}': {}", organization,
                    teamsResult.getError());
            throw new SentryApiException("Cannot get teams of organization %s: %s",
                    organization,
                    teamsResult.getError().getType());
        }
        boolean teamExists = false;
        for (TeamInfo teamInfo : teamsResult.get()) {
            if (teamInfo.getSlug().equals(organization)) {
                teamExists = true;
                break;
            }
        }
        if (!teamExists) {
            Result<TeamInfo, ErrorInfo> creationResult = sentryApiClient.createTeam(organization,
                    organization);
            if (!creationResult.isOk()) {
                LOGGER.warn("Cannot create default team in organization '{}': {}", organization,
                        creationResult.getError());
                throw new SentryApiException("Cannot create default team in organization %s: %s",
                        organization,
                        creationResult.getError().getType());
            }
            LOGGER.info("Team '{}' is created in organization '{}'", organization, organization);
        }
        return organization;
    }

    public void update() {
    }
}
