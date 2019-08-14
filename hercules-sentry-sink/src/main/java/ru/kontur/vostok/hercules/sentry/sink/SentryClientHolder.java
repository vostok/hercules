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
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

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

    private static final String SLUG_REGEX = "[a-z0-9_\\-]{1,50}";
    private static final Validator<String> slugValidator = StringValidators.matchesWith(SLUG_REGEX);

    /**
     * Clients is a base of Sentry clients and their organizations and projects.<p>
     * It is a cache which stores the Map with the String of organization as a key
     * and the Map as a value. <p>
     * The nested Map matching this organization contains the String of project as a key
     * and the {@link SentryClient} as a value.
     */
    private volatile Map<String, Map<String, SentryClient>> clients = Collections.emptyMap();

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
     * the method create the organization or the project in the Sentry and in the cache.
     * and then makes one another attempt to get Sentry client from the cache
     * <p>
     * @param organization the organization
     * @param project      the project
     * @return the {@link Optional} describing SentryClient matching an organization and a project
     * or empty {@link Optional} if cannot get or create client
     */
    public Result<SentryClient, ErrorInfo> getOrCreateClient(String organization, String project) {
        Result<SentryClient, ErrorInfo> sentryClientResult = null;
        boolean triedToCreateClient = false;
        boolean success = false;
        while (!success) {
            sentryClientResult = getClient(organization, project);
            if (sentryClientResult.isOk()) {
                success = true;
            } else {
                Result<Void, ErrorInfo> validationResult = validateSlugs(organization, project);
                if (!validationResult.isOk()) {
                    sentryClientResult = Result.error(validationResult.getError());
                    break;
                }
                Result<Void, ErrorInfo> orgExists = createOrganizationIfNotExists(organization);
                if (!orgExists.isOk()) {
                    sentryClientResult = Result.error(orgExists.getError());
                    break;
                }
                Result<Void, ErrorInfo> projectExists = createProjectIfNotExists(organization, project);
                if (!projectExists.isOk()) {
                    sentryClientResult = Result.error(projectExists.getError());
                    break;
                }
                if (triedToCreateClient) {
                    LOGGER.error("Cannot find Sentry client in cache and cannot create new Sentry client");
                    sentryClientResult = Result.error(new ErrorInfo(true));
                    break;
                }
                triedToCreateClient = true;
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
    public Result<SentryClient, ErrorInfo> getClient(String organization, String project) {
        Map<String, SentryClient> projectMap = clients.get(organization);
        if (projectMap == null) {
            String message = String.format("The organization '%s' is not found in the cache", organization);
            LOGGER.info(message);
            return Result.error(new ErrorInfo(message));
        }
        SentryClient sentryClient = projectMap.get(project);
        if (sentryClient == null) {
            String message = String.format("The client for the project '%s' in the organization '%s' is not found in the cache",
                    project, organization);
            LOGGER.info(message);
            return Result.error(new ErrorInfo(message));
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
    public Result<Void, ErrorInfo> validateSlugs(String... slugs) {
        Result<Void, ErrorInfo> result = Result.ok();
        for(String slug : slugs) {
            ValidationResult validationResult = slugValidator.validate(slug);
            if (validationResult.isError()) {
                LOGGER.error(String.format("Invalid name: '%s'. This name cannot be Sentry slug: %s", slug, validationResult.error()));
                return Result.error(new ErrorInfo(false));
            }
        }
        return result;
    }

    /**
     * Check the organization existence in the Sentry.
     * If the organization does not exist the method create new organization in the Sentry and in the cache
     *
     * @param organization the organization
     * @return the {@link Result} object with success information
     */
    public Result<Void, ErrorInfo> createOrganizationIfNotExists(String organization) {
        Result<List<OrganizationInfo>, ErrorInfo> getListResult = sentryApiClient.getOrganizations();
        if(!getListResult.isOk()) {
            LOGGER.error("Cannot get organizations from Sentry: {}", getListResult.getError());
            return Result.error(getListResult.getError());
        }
        boolean orgExists = false;
        for(OrganizationInfo organizationInfo : getListResult.get()) {
            if(organizationInfo.getSlug().equals(organization)) {
                orgExists = true;
                break;
            }
        }
        if (!orgExists) {
            Result<OrganizationInfo, ErrorInfo> creationResult = sentryApiClient.createOrganization(organization);
            if(!creationResult.isOk()) {
                LOGGER.warn(String.format("Cannot create organisation '%s': {}", organization), creationResult.getError());
                return Result.error(creationResult.getError());
            }
        }
        if (!clients.containsKey(organization)) {
            clients.put(organization, new ConcurrentHashMap<>());
            LOGGER.info(String.format("Organisation '%s' is uploaded into cache", organization));
        }
        return Result.ok();
    }

    /**
     * Check the project existence in the organization in the Sentry.
     * If the project does not exist the method create new project in the Sentry and in the cache.
     * For project creation the method find or create default team.
     * The method check existence of dsn-key for the project and create dsn-key in case of its absence.
     * The method returns the {@link Result} with error if project does not have dsn-key.
     *
     * @param organization the organization
     * @param project the project
     * @return the {@link Result} object with success information.
     */
    public Result<Void, ErrorInfo> createProjectIfNotExists(String organization, String project) {
        Result<List<ProjectInfo>, ErrorInfo> getListResult = sentryApiClient.getProjects(organization);
        if (!getListResult.isOk()) {
            LOGGER.error(String.format("Cannot get projects of organization '%s': {}", organization), getListResult.getError());
            return Result.error(getListResult.getError());
        }
        boolean projExists = false;
        for (ProjectInfo projectInfo : getListResult.get()) {
            if (projectInfo.getSlug().equals(project)) {
                projExists = true;
                break;
            }
        }
        if (!projExists) {
            Result<Void, ErrorInfo> defaultTeamExists = createDefaultTeamIfNotExists(organization);
            if (!defaultTeamExists.isOk()) {
                return Result.error(defaultTeamExists.getError());
            }
            String team = organization;
            Result<ProjectInfo, ErrorInfo> creationResult = sentryApiClient.createProject(organization, team, project);
            if (!creationResult.isOk()) {
                LOGGER.warn(String.format("Cannot create project '%s' in organization '%s': {}",project, organization), creationResult.getError());
                return Result.error(creationResult.getError());
            }
        }
        Result<String, ErrorInfo> dsnResult = getDsnKey(organization, project);
        if (dsnResult.isOk()){
            SentryClient sentryClient = SentryClientFactory.sentryClient(applySettings(dsnResult.get()), sentryClientFactory);
            clients.get(organization).put(project, sentryClient);
            LOGGER.info(String.format("The client for project '%s' is uploaded into cache", project));
        } else {
            return Result.error(dsnResult.getError());
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
    public Result<Void, ErrorInfo> createDefaultTeamIfNotExists(String organization) {
        Result<List<TeamInfo>, ErrorInfo> getListResult = sentryApiClient.getTeams(organization);
        if (!getListResult.isOk()) {
            LOGGER.error(String.format("Cannot get teams of organization '%s': {}", organization), getListResult.getError());
            return Result.error(getListResult.getError());
        }
        String team = organization;
        boolean teamExists = false;
        for (TeamInfo teamInfo : getListResult.get()) {
            if (teamInfo.getSlug().equals(team)) {
                teamExists = true;
                break;
            }
        }
        if (!teamExists) {
            Result<TeamInfo, ErrorInfo> creationResult = sentryApiClient.createTeam(organization, team);
            if (!creationResult.isOk()) {
                LOGGER.warn(String.format("Cannot create default team in organization '%s': {}", organization), creationResult.getError());
                return Result.error(creationResult.getError());
            }
        }
        return Result.ok();
    }

    /**
     * Remove Sentry client from cache
     *
     * @param organization the organization
     * @param project the project
     */
    public void removeClientFromCache(String organization, String project) {
        clients.get(organization).remove(project);
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
            Result<List<OrganizationInfo>, ErrorInfo> organizations = sentryApiClient.getOrganizations();
            if (!organizations.isOk()) {
                LOGGER.error("Cannot update organizations info due to: {}", organizations.getError());
                return;
            }

            Map<String, Map<String, SentryClient>> organizationMap = new ConcurrentHashMap<>();
            for (OrganizationInfo organizationInfo : organizations.get()) {
                String organization = organizationInfo.getSlug();

                Result<List<ProjectInfo>, ErrorInfo> projects = sentryApiClient.getProjects(organization);
                if (!projects.isOk()) {
                    LOGGER.error("Cannot update projects info due to: {}", projects.getError());
                    return;
                }

                Map<String, SentryClient> projectMap = new ConcurrentHashMap<>();
                organizationMap.put(organization, projectMap);

                for (ProjectInfo projectInfo : projects.get()) {
                    String project = projectInfo.getSlug();

                    Result<String, ErrorInfo> dsnResult = getDsnKey(organization, project);
                    if (dsnResult.isOk()) {
                        String dsnString = dsnResult.get();
                        SentryClient sentryClient = SentryClientFactory.sentryClient(applySettings(dsnString), sentryClientFactory);
                        projectMap.put(project, sentryClient);
                    }
                }
            }

            clients = organizationMap;
        } catch (Throwable t) {
            LOGGER.error("Error of updating Sentry clients: {}", t.getMessage());
            System.exit(1);
        }
    }

    private Result<String, ErrorInfo> getDsnKey(String organization, String project) {
        Result<List<KeyInfo>, ErrorInfo> publicDsn = sentryApiClient.getPublicDsn(organization, project);
        if (publicDsn.isOk()) {
            Optional<String> dsn = publicDsn.get().stream()
                    .filter(KeyInfo::isActive)
                    .findFirst()
                    .map(KeyInfo::getDsn)
                    .map(DsnInfo::getPublicDsn);
            if (dsn.isPresent()) {
                String dsnString = dsn.get();
                try {
                    new URL(dsnString);
                } catch (MalformedURLException e) {
                    LOGGER.error(String.format("Malformed dsn '%s', there might be an error in sentry configuration", dsnString));
                    return Result.error(new ErrorInfo(false));
                }
                return Result.ok(dsnString);
            } else {
                LOGGER.error(String.format("Active dsn is not present for project %s", project));
                return Result.error(new ErrorInfo(false));
            }
        } else {
            return Result.error(publicDsn.getError());
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
