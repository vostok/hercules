package ru.kontur.vostok.hercules.sentry.client.impl.v9.connector;

import io.sentry.DefaultSentryClientFactory;
import io.sentry.SentryClient;
import io.sentry.SentryClientFactory;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.sentry.client.SentryConnector;
import ru.kontur.vostok.hercules.sentry.client.SentryConnectorHolder;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;
import ru.kontur.vostok.hercules.sentry.client.ErrorInfo;
import ru.kontur.vostok.hercules.sentry.client.api.SentryApiClient;
import ru.kontur.vostok.hercules.sentry.client.api.model.DsnInfo;
import ru.kontur.vostok.hercules.sentry.client.api.model.KeyInfo;
import ru.kontur.vostok.hercules.sentry.client.api.model.OrganizationInfo;
import ru.kontur.vostok.hercules.sentry.client.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.sentry.client.api.model.TeamInfo;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.validation.StringValidators;
import ru.kontur.vostok.hercules.util.validation.ValidationResult;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sentry connector holder.
 * The class stores actual Sentry connectors for event sending to the Sentry
 *
 * @author Petr Demenev
 */
public class SentryConnectorHolderImplV9 implements SentryConnectorHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentryConnectorHolderImplV9.class);

    private static final String DISABLE_UNCAUGHT_EXCEPTION_HANDLING = DefaultSentryClientFactory.UNCAUGHT_HANDLER_ENABLED_OPTION + "=false";
    private static final String DISABLE_IN_APP_WARN_MESSAGE = DefaultSentryClientFactory.IN_APP_FRAMES_OPTION + "=%20"; // Empty value disables warn message

    private static final String SLUG_REGEX = "[a-z0-9_\\-]{1,50}";
    private static final Validator<String> slugValidator = StringValidators.matchesWith(SLUG_REGEX);

    /**
     * Connectors is a base of Sentry clients and their organizations and projects.<p>
     * It is a cache which stores the Map with the String of organization as a key
     * and the Map as a value. <p>
     * The nested Map matching this organization contains the String of project as a key
     * and the {@link SentryConnector} as a value.
     */
    private volatile ConcurrentMap<String, ConcurrentMap<String, SentryConnectorImplV9>> connectors = new ConcurrentHashMap<>();
    private final SentryApiClient sentryApiClient;
    private final SentryClientFactory sentryClientFactory;

    public SentryConnectorHolderImplV9(SentryApiClient sentryApiClient, Properties senderProperties) {
        this.sentryApiClient = sentryApiClient;
        this.sentryClientFactory = new HerculesClientFactory(senderProperties);
    }

    /**
     * Get or create Sentry connector by pair of an organization and a project.
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
    public Result<SentryConnectorImplV9, ErrorInfo> getOrCreateConnector(String organization, String project) {
        ConcurrentMap<String, SentryConnectorImplV9> projectMap = connectors.get(organization);
        if (projectMap == null) {
            Result<Void, ErrorInfo> nameValidationResult = validateSlug(organization);
            if (!nameValidationResult.isOk()) {
                return Result.error(nameValidationResult.getError());
            }
            Result<ConcurrentMap<String, SentryConnectorImplV9>, ErrorInfo> orgExists = createOrganizationIfNotExists(organization);
            if (!orgExists.isOk()) {
                return Result.error(orgExists.getError());
            }
            projectMap = orgExists.get();
        }
        SentryConnectorImplV9 connector = projectMap.get(project);
        if (!projectMap.containsKey(project)) {
            Result<Void, ErrorInfo> nameValidationResult = validateSlug(project);
            if (!nameValidationResult.isOk()) {
                return Result.error(nameValidationResult.getError());
            }
            Result<SentryConnectorImplV9, ErrorInfo> projectExists = createProjectIfNotExists(organization, project);
            if (!projectExists.isOk()) {
                return Result.error(projectExists.getError());
            }
            connector = projectExists.get();
        }
        if (!connector.isPresent()) {
            return Result.error(new ErrorInfo("NoActiveDSN", false));
        }
        return Result.ok(connector);
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
    private Result<ConcurrentMap<String, SentryConnectorImplV9>, ErrorInfo> createOrganizationIfNotExists(String organization) {
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
        ConcurrentMap<String, SentryConnectorImplV9> projectMap = connectors.get(organization);
        if (projectMap == null) {
            projectMap = new ConcurrentHashMap<>();
            connectors.put(organization, projectMap);
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
     * @return the {@link Result} object with {@link SentryConnector} object
     */
    private Result<SentryConnectorImplV9, ErrorInfo> createProjectIfNotExists(String organization, String project) {
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
        Result<SentryConnectorImplV9, ErrorInfo> clientResult = createClient(organization, project);
        if (!clientResult.isOk()) {
            return Result.error(clientResult.getError());
        }
        connectors.get(organization).put(project, clientResult.get());
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
     * @return the {@link Result} object with {@link SentryConnector} which contains {@link SentryClient}
     * or with empty {@link SentryConnector} if active client does nor exist
     */
    private Result<SentryConnectorImplV9, ErrorInfo> createClient(String organization, String project) {
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
            return Result.ok(SentryConnectorImplV9.of(sentryClient));
        } else {
            return Result.ok(SentryConnectorImplV9.empty());
        }
    }

    /**
     * Remove Sentry client from cache
     *
     * @param destination Sentry destination (contains organization and project)
     */
    public void removeClientFromCache(SentryDestination destination) {
        String organization = destination.organization();
        String project = destination.project();
        connectors.get(organization).put(project, SentryConnectorImplV9.empty());
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

            ConcurrentMap<String, ConcurrentMap<String, SentryConnectorImplV9>> organizationMap = new ConcurrentHashMap<>();
            for (OrganizationInfo organizationInfo : organizations.get()) {
                String organization = organizationInfo.getSlug();

                Result<List<ProjectInfo>, ErrorInfo> projects = sentryApiClient.getProjects(organization);
                if (!projects.isOk()) {
                    LOGGER.error("Cannot update projects info due to: {}", projects.getError());
                    throw new IllegalArgumentException(organizations.getError().toString());
                }

                ConcurrentMap<String, SentryConnectorImplV9> projectMap = new ConcurrentHashMap<>();
                organizationMap.put(organization, projectMap);

                for (ProjectInfo projectInfo : projects.get()) {
                    String project = projectInfo.getSlug();

                    Result<SentryConnectorImplV9, ErrorInfo> clientResult = createClient(organization, project);
                    if (clientResult.isOk()) {
                        SentryConnectorImplV9 client = clientResult.get();
                        projectMap.put(project, client);
                    }
                }
            }

            connectors = organizationMap;
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
