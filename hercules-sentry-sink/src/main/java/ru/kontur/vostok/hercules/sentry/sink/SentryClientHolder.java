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
import ru.kontur.vostok.hercules.sentry.sink.client.ClientWrapper;
import ru.kontur.vostok.hercules.sentry.sink.client.HerculesClientFactory;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.validation.StringValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
     * and the {@link ClientWrapper} as a value.
     */
    private volatile ConcurrentMap<String, ConcurrentMap<String, ClientWrapper>> clients = new ConcurrentHashMap<>();
    private final SentryApiClient sentryApiClient;
    private final SentryClientFactory sentryClientFactory;

    public SentryClientHolder(SentryApiClient sentryApiClient, Properties senderProperties) {
        this.sentryApiClient = sentryApiClient;
        this.sentryClientFactory = new HerculesClientFactory(senderProperties);
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
     * <p>
     * @param organization the organization
     * @param project      the project
     * @return the ok {@link Result} with {@link SentryClient} matching an organization and a project
     * or error Result if cannot get or create client
     */
    public Result<SentryClient, ErrorInfo> getOrCreateClient(String organization, String project) {
        ConcurrentMap<String, ClientWrapper> projectMap = clients.get(organization);
        if (projectMap == null) {
            Result<Void, ErrorInfo> nameValidationResult = validateSlug(organization);
            if (!nameValidationResult.isOk()) {
                return Result.error(nameValidationResult.getError());
            }
            Result<ConcurrentMap<String, ClientWrapper>, ErrorInfo> orgExists = createOrganizationIfNotExists(organization);
            if (!orgExists.isOk()) {
                return Result.error(orgExists.getError());
            }
            projectMap = orgExists.get();
        }
        ClientWrapper client = projectMap.get(project);
        if (!projectMap.containsKey(project)) {
            Result<Void, ErrorInfo> nameValidationResult = validateSlug(project);
            if (!nameValidationResult.isOk()) {
                return Result.error(nameValidationResult.getError());
            }
            Result<ClientWrapper, ErrorInfo> projectExists = createProjectIfNotExists(organization, project);
            if (!projectExists.isOk()) {
                return Result.error(projectExists.getError());
            }
            client = projectExists.get();
        }
        if (!client.isPresent()) {
            return Result.error(new ErrorInfo("NoActiveDSN", false));
        }
        return Result.ok(client.get());
    }

    /**
     * Validate string that can be used as Sentry slug.
     * This string must match requirements of Sentry slug
     *
     * @param slug the string
     * @return the {@link Result} object with success information
     */
    private Result<Void, ErrorInfo> validateSlug(String slug) {
        ValidationResult validationResult = slugValidator.validate(slug);
        if (validationResult.isError()) {
            LOGGER.warn("Invalid name: '{}'. This name cannot be Sentry slug: {}", slug, validationResult.error());
            return Result.error(new ErrorInfo("SlugValidationError", false));
        }
        return Result.ok();
    }

    /**
     * Check the organization existence in the Sentry.
     * If the organization does not exist the method create new organization in the Sentry and in the cache
     *
     * @param organization the organization
     * @return the {@link Result} object with project map corresponding this organization
     */
    private Result<ConcurrentMap<String, ClientWrapper>, ErrorInfo> createOrganizationIfNotExists(String organization) {
        Result<List<OrganizationInfo>, ErrorInfo> organizationsResult = sentryApiClient.getOrganizations();
        if(!organizationsResult.isOk()) {
            LOGGER.error("Cannot get organizations from Sentry: {}", organizationsResult.getError());
            return Result.error(organizationsResult.getError());
        }
        boolean orgExists = false;
        for(OrganizationInfo organizationInfo : organizationsResult.get()) {
            if(organizationInfo.getSlug().equals(organization)) {
                orgExists = true;
                break;
            }
        }
        if (!orgExists) {
            Result<OrganizationInfo, ErrorInfo> creationResult = sentryApiClient.createOrganization(organization);
            if(!creationResult.isOk()) {
                LOGGER.warn("Cannot create organization '{}': {}", organization, creationResult.getError());
                return Result.error(creationResult.getError());
            }
            LOGGER.info("Organization '{}' is created", organization);
        }
        ConcurrentMap<String, ClientWrapper> projectMap = clients.get(organization);
        if (projectMap == null) {
            projectMap = new ConcurrentHashMap<>();
            clients.put(organization, projectMap);
            LOGGER.info("Organization '{}' is uploaded into cache", organization);
        }
        return Result.ok(projectMap);
    }

    /**
     * Check the project existence in the organization in the Sentry.
     * If the project does not exist the method create new project in the Sentry and in the cache.
     * For project creation the method find or create default team.
     * The method check existence of client for the project and create client in case of its absence.
     *
     * @param organization the organization
     * @param project the project
     * @return the {@link Result} object with {@link ClientWrapper} object
     */
    private Result<ClientWrapper, ErrorInfo> createProjectIfNotExists(String organization, String project) {
        Result<List<ProjectInfo>, ErrorInfo> projectsResult = sentryApiClient.getProjects(organization);
        if (!projectsResult.isOk()) {
            LOGGER.error("Cannot get projects of organization '{}': {}", organization, projectsResult.getError());
            return Result.error(projectsResult.getError());
        }
        boolean projExists = false;
        for (ProjectInfo projectInfo : projectsResult.get()) {
            if (projectInfo.getSlug().equals(project)) {
                projExists = true;
                break;
            }
        }
        if (!projExists) {
            Result<String, ErrorInfo> defaultTeamExists = createDefaultTeamIfNotExists(organization);
            if (!defaultTeamExists.isOk()) {
                return Result.error(defaultTeamExists.getError());
            }
            String team = defaultTeamExists.get();
            Result<ProjectInfo, ErrorInfo> creationResult = sentryApiClient.createProject(organization, team, project);
            if (!creationResult.isOk()) {
                LOGGER.warn("Cannot create project '{}' in organization '{}': {}", project, organization,
                        creationResult.getError());
                return Result.error(creationResult.getError());
            }
            LOGGER.info("Project '{}' is created in organization '{}'", project, organization);
        }
        Result<ClientWrapper, ErrorInfo> clientResult = createClient(organization, project);
        if (!clientResult.isOk()) {
            return Result.error(clientResult.getError());
        }
        clients.get(organization).put(project, clientResult.get());
        LOGGER.info("Project '{}' in organization  '{}' is uploaded into cache", project, organization);
        return clientResult;
    }

    /**
     * Check default team existence in the organization in the Sentry.
     * If default team does not exist the method create new team
     *
     * @param organization the organization
     * @return the {@link Result} object with team name.
     */
    private Result<String, ErrorInfo> createDefaultTeamIfNotExists(String organization) {
        Result<List<TeamInfo>, ErrorInfo> teamsResult = sentryApiClient.getTeams(organization);
        if (!teamsResult.isOk()) {
            LOGGER.error("Cannot get teams of organization '{}': {}", organization, teamsResult.getError());
            return Result.error(teamsResult.getError());
        }
        String team = organization;
        boolean teamExists = false;
        for (TeamInfo teamInfo : teamsResult.get()) {
            if (teamInfo.getSlug().equals(team)) {
                teamExists = true;
                break;
            }
        }
        if (!teamExists) {
            Result<TeamInfo, ErrorInfo> creationResult = sentryApiClient.createTeam(organization, team);
            if (!creationResult.isOk()) {
                LOGGER.warn("Cannot create default team in organization '{}': {}", organization,
                        creationResult.getError());
                return Result.error(creationResult.getError());
            }
            LOGGER.info("Team '{}' is created in organization '{}'", team, organization);
        }
        return Result.ok(team);
    }

    /**
     * Create client for specified project by DSN-key from Sentry
     *
     * @param organization the organization with specified project
     * @param project the project
     * @return the {@link Result} object with {@link ClientWrapper} which contains {@link SentryClient}
     * or with empty {@link ClientWrapper} if active client does nor exist
     */
    private Result<ClientWrapper, ErrorInfo> createClient(String organization, String project) {
        Result<List<KeyInfo>, ErrorInfo> keysResult = sentryApiClient.getPublicDsn(organization, project);
        if (!keysResult.isOk()) {
            LOGGER.error("Cannot get DSN keys for project '{}' in organization '{}': {}", project, organization,
                    keysResult.getError());
            return Result.error(keysResult.getError());
        }
        Optional<String> dsnOptional = keysResult.get().stream()
                .filter(KeyInfo::isActive)
                .findFirst()
                .map(KeyInfo::getDsn)
                .map(DsnInfo::getPublicDsn);
        if (dsnOptional.isPresent()) {
            String dsnString = dsnOptional.get();
            try {
                new URL(dsnString);
            } catch (MalformedURLException e) {
                LOGGER.error("Malformed dsn '{}', there might be an error in sentry configuration", dsnString);
                return Result.error(new ErrorInfo("MalformedDsn", false));
            }
            SentryClient sentryClient = SentryClientFactory.sentryClient(applySettings(dsnString), sentryClientFactory);
            return Result.ok(ClientWrapper.of(sentryClient));
        } else {
            return Result.ok(ClientWrapper.empty());
        }
    }

    /**
     * Remove Sentry client from cache
     *
     * @param organization the organization
     * @param project the project
     */
    public void removeClientFromCache(String organization, String project) {
        clients.get(organization).put(project, ClientWrapper.empty());
        LOGGER.info("The client is removed from cache for project '{}' in organization '{}'", project, organization);
    }

    /**
     * Fully update clients in cache in this class by information about project clients from Sentry.
     * <p>
     * This method firstly updates organizations,<p>
     * then updates projects of every organization,<p>
     * then updates clients of every project.
     */
    public void update() {
        try {
            LOGGER.info("Updating Sentry clients");
            Result<List<OrganizationInfo>, ErrorInfo> organizations = sentryApiClient.getOrganizations();
            if (!organizations.isOk()) {
                LOGGER.error("Cannot update organizations info due to: {}", organizations.getError());
                throw new IllegalArgumentException(organizations.getError().toString());
            }

            ConcurrentMap<String, ConcurrentMap<String, ClientWrapper>> organizationMap = new ConcurrentHashMap<>();
            for (OrganizationInfo organizationInfo : organizations.get()) {
                String organization = organizationInfo.getSlug();

                Result<List<ProjectInfo>, ErrorInfo> projects = sentryApiClient.getProjects(organization);
                if (!projects.isOk()) {
                    LOGGER.error("Cannot update projects info due to: {}", projects.getError());
                    throw new IllegalArgumentException(organizations.getError().toString());
                }

                ConcurrentMap<String, ClientWrapper> projectMap = new ConcurrentHashMap<>();
                organizationMap.put(organization, projectMap);

                for (ProjectInfo projectInfo : projects.get()) {
                    String project = projectInfo.getSlug();

                    Result<ClientWrapper, ErrorInfo> clientResult = createClient(organization, project);
                    if (clientResult.isOk()) {
                        ClientWrapper client = clientResult.get();
                        projectMap.put(project, client);
                    }
                }
            }

            clients = organizationMap;
        } catch (Throwable t) {
            LOGGER.error("Error of updating Sentry clients: {}", t.getMessage());
            throw t;
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
