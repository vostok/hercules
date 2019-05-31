package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.DefaultSentryClientFactory;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.sentry.api.model.DsnInfo;
import ru.kontur.vostok.hercules.sentry.api.model.KeyInfo;
import ru.kontur.vostok.hercules.sentry.api.model.OrganizationInfo;
import ru.kontur.vostok.hercules.sentry.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.sentry.api.model.TeamInfo;
import ru.kontur.vostok.hercules.sentry.sink.client.HerculesClientFactory;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.validation.StringValidators;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sentry client holder.
 * The class stores actual Sentry clients for event sending to the Sentry
 *
 * @author Petr Demenev
 */
public class SentryClientHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentryClientHolder.class);

    private static final String DISABLE_UNCAUGHT_EXCEPTION_HANDLING = DefaultSentryClientFactory.UNCAUGHT_HANDLER_ENABLED_OPTION + "=false";
    private static final String DISABLE_IN_APP_WARN_MESSAGE = DefaultSentryClientFactory.IN_APP_FRAMES_OPTION + "=%20"; // Empty value disables warn message

    private static final String SLUG_REGEX = "[a-z0-9_\\-]+";
    private static final Validator<String> slugValidator = StringValidators.matchesWith(SLUG_REGEX);

    /**
     * Clients is a {@link AtomicReference} with a base of Sentry clients and their organizations and projects.<p>
     * It is a cache which stores the Map with the String of organization as a key
     * and the Map as a value. <p>
     * The nested Map matching this organization contains the String of project as a key
     * and the {@link SentryClient} as a value.
     */
    private final AtomicReference<Map<String, Map<String, SentryClient>>> clients = new AtomicReference<>(Collections.emptyMap());

    private final SentryApiClient sentryApiClient;
    private final SentryClientFactory sentryClientFactory = new HerculesClientFactory();

    public SentryClientHolder(SentryApiClient sentryApiClient) {
        this.sentryApiClient = sentryApiClient;
    }

    /**
     * Get or create Sentry client by pair of an organization and a project.
     * <p>
     * If the organization and the project of event exist in cache,
     * it is the case of the cheapest and simplest operation.
     * <p>
     * If the organization or the project of event does not exist in the cache,
     * the method finds the organization and the project in the Sentry.
     * If the organization or the project does not exist in the Sentry,
     * the method create the organization or the project in the Sentry.
     * Then the method updates the cache from the Sentry,
     * and then makes one another attempt to get Sentry client from the cache
     * <p>
     * @param organization the organization
     * @param project      the project
     * @return the {@link Optional} describing SentryClient matching an organization and a project
     * or empty {@link Optional} if cannot get or create client
     */
    public Result<SentryClient, SentrySinkError> getOrCreateClient(String organization, String project) {
        Result<SentryClient, SentrySinkError> sentryClientResult = null;
        boolean triedToUpdate = false;
        boolean success = false;
        while (!success) {
            sentryClientResult = getClient(organization, project);
            if (sentryClientResult.isOk()) {
                success = true;
            } else {
                LOGGER.info(String.format("Sentry client is not found in cache for project '%s' in organization '%s'", project, organization));
                Result<Void, SentrySinkError> validationResult = validateSlugs(organization, project);
                if (!validationResult.isOk()) {
                    sentryClientResult = Result.error(validationResult.getError());
                    break;
                }
                Result<Void, SentrySinkError> orgExists = createOrganizationIfNotExists(organization);
                if (!orgExists.isOk()) {
                    sentryClientResult = Result.error(orgExists.getError());
                    break;
                }
                Result<Void, SentrySinkError> projectExists = createProjectIfNotExists(organization, project);
                if (!projectExists.isOk()) {
                    sentryClientResult = Result.error(projectExists.getError());
                    break;
                }
                if (triedToUpdate) {
                    String message = "Error due to updating Sentry clients into Sentry Sink cache";
                    LOGGER.error(message);
                    sentryClientResult = Result.error(new SentrySinkError(message, false));
                    break;
                }
                update();
                triedToUpdate = true;
            }
        }
        return sentryClientResult;
    }

    /**
     * Get Sentry client matching an organization and a project from cache in this class
     *
     * @param organization the organization
     * @param project the project
     * @return the {@link Optional} describing SentryClient
     * or empty {@link Optional} if cannot get client from cache
     */
    public Result<SentryClient, SentrySinkError> getClient(String organization, String project) {
        Map<String, SentryClient> projectMap = clients.get().get(organization);
        if (projectMap == null) {
            String message = String.format("The organization '%s' is not found in the cache", organization);
            LOGGER.info(message);
            return Result.error(new SentrySinkError(message));
        }
        SentryClient sentryClient = projectMap.get(project);
        if (sentryClient == null) {
            String message = String.format("The project '%s' is not found in the cache", project);
            LOGGER.info(message);
            return Result.error(new SentrySinkError(message));
        }
        return Result.ok(sentryClient);
    }

    /**
     * Validate strings that can be used as Sentry slugs.
     * This strings must match requirements of Sentry slugs
     *
     * @param slugs the strings
     * @return the {@link Result} object with success information
     */
    public Result<Void, SentrySinkError> validateSlugs(String... slugs) {
        Result<Void, SentrySinkError> result = Result.ok();
        for(String slug : slugs) {
            Optional<String> slugError = slugValidator.validate(slug);
            if (slugError.isPresent()) {
                String message = String.format("Invalid name: '%s'. This name cannot be Sentry slug: %s", slug, slugError.get());
                LOGGER.error(message);
                return Result.error(new SentrySinkError(message, false));
            }
        }
        return result;
    }

    /**
     * Check the organization existence in the Sentry.
     * If the organization does not exist the method create new organization
     *
     * @param organization the organization
     * @return the {@link Result} object with success information
     */
    public Result<Void, SentrySinkError> createOrganizationIfNotExists(String organization) {
        Result<List<OrganizationInfo>, SentrySinkError> getListResult = sentryApiClient.getOrganizations();
        if(!getListResult.isOk()) {
            LOGGER.error("Cannot get organizations from Sentry: {}", getListResult.getError());
            return Result.error(getListResult.getError());
        }
        for(OrganizationInfo organizationInfo : getListResult.get()) {
            if(organizationInfo.getSlug().equals(organization)) {
                return Result.ok();
            }
        }
        Result<OrganizationInfo, SentrySinkError> creationResult = sentryApiClient.createOrganization(organization);
        if(!creationResult.isOk()) {
            LOGGER.warn(String.format("Cannot create organisation '%s': {}", organization), creationResult.getError());
            return Result.error(creationResult.getError());
        }
        return Result.ok();
    }

    /**
     * Check the project existence in the organization in the Sentry.
     * If the project does not exist the method create new project.
     * For project creation the method find or create default team
     *
     * @param organization the organization
     * @param project the project
     * @return the {@link Result} object with success information.
     */
    public Result<Void, SentrySinkError> createProjectIfNotExists(String organization, String project) {
        Result<List<ProjectInfo>, SentrySinkError> getListResult = sentryApiClient.getProjects(organization);
        if (!getListResult.isOk()) {
            LOGGER.error(String.format("Cannot get projects of organization '%s': {}", organization), getListResult.getError());
            return Result.error(getListResult.getError());
        }
        for (ProjectInfo projectInfo : getListResult.get()) {
            if (projectInfo.getSlug().equals(project)) {
                return Result.ok();
            }
        }
        Result<Void, SentrySinkError> defaultTeamExists = createDefaultTeamIfNotExists(organization);
        if (!defaultTeamExists.isOk()) {
            return Result.error(defaultTeamExists.getError());
        }
        String team = organization;
        Result<ProjectInfo, SentrySinkError> creationResult = sentryApiClient.createProject(organization, team, project);
        if (!creationResult.isOk()) {
            LOGGER.warn(String.format("Cannot create project '%s' in organization '%s': {}",project, organization), creationResult.getError());
            return Result.error(creationResult.getError());
        }
        return Result.ok();
    }

    /**
     * Check default team existence in the organization in the Sentry.
     * If default team does not exist the method create new team
     *
     * @param organization the organization
     * @return the {@link Result} object with success information.
     */
    public Result<Void, SentrySinkError> createDefaultTeamIfNotExists(String organization) {
        Result<List<TeamInfo>, SentrySinkError> getListResult = sentryApiClient.getTeams(organization);
        if (!getListResult.isOk()) {
            LOGGER.error(String.format("Cannot get teams of organization '%s': {}", organization), getListResult.getError());
            return Result.error(getListResult.getError());
        }
        String team = organization;
        for (TeamInfo teamInfo : getListResult.get()) {
            if (teamInfo.getSlug().equals(team)) {
                return Result.ok();
            }
        }
        Result<TeamInfo, SentrySinkError> creationResult = sentryApiClient.createTeam(organization, team);
        if (!creationResult.isOk()) {
            LOGGER.warn(String.format("Cannot create default team in organization '%s': {}", organization), creationResult.getError());
            return Result.error(creationResult.getError());
        }
        return Result.ok();
    }

    /**
     * Update clients in this class by information about project clients from Sentry.
     * <p>
     * This method firstly updates organizations,<p>
     * then updates projects of every organization,<p>
     * then updates dsn-keys of every project,<p>
     * then updates clients by dsn-keys.
     */
    public void update() {
        try {
            LOGGER.info("Updating Sentry clients");
            Result<List<OrganizationInfo>, SentrySinkError> organizations = sentryApiClient.getOrganizations();
            if (!organizations.isOk()) {
                LOGGER.error("Cannot update organizations info due to: {}", organizations.getError());
                return;
            }

            Map<String, Map<String, SentryClient>> organizationMap = new HashMap<>();
            for (OrganizationInfo organizationInfo : organizations.get()) {
                String organization = organizationInfo.getSlug();

                Result<List<ProjectInfo>, SentrySinkError> projects = sentryApiClient.getProjects(organization);
                if (!projects.isOk()) {
                    LOGGER.error("Cannot update projects info due to: {}", projects.getError());
                    return;
                }

                Map<String, SentryClient> projectMap = new HashMap<>();
                organizationMap.put(organization, projectMap);

                for (ProjectInfo projectInfo : projects.get()) {
                    String project = projectInfo.getSlug();

                    Result<List<KeyInfo>, SentrySinkError> publicDsn = sentryApiClient.getPublicDsn(organization, project);
                    if (publicDsn.isOk()) {
                        Optional<String> dsn = publicDsn.get().stream()
                                .findAny()
                                .map(KeyInfo::getDsn)
                                .map(DsnInfo::getPublicDsn);
                        if (dsn.isPresent()) {
                            String dsnString = dsn.get();
                            try {
                                new URL(dsnString);
                            } catch (MalformedURLException e) {
                                throw new Exception(String.format("Malformed dsn '%s', there might be an error in sentry configuration", dsnString));
                            }
                            SentryClient sentryClient = SentryClientFactory.sentryClient(applySettings(dsnString), sentryClientFactory);
                            projectMap.put(project, sentryClient);
                        }
                    }
                }
            }

            clients.set(organizationMap);
        } catch (Throwable t) {
            LOGGER.error("Error of updating Sentry clients: {}", t.getMessage());
            System.exit(1);
        }
    }

    /**
     * Apply settings to dsn
     * Sentry uses dsn to pass properties to client
     *
     * @param dsn the source dsn
     * @return the dsn with settings
     */
    private String applySettings(String dsn) {
        return dsn + "?" + String.join("&",
                DISABLE_UNCAUGHT_EXCEPTION_HANDLING,
                DISABLE_IN_APP_WARN_MESSAGE
        );
    }
}
