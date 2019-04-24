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
import ru.kontur.vostok.hercules.sentry.sink.client.CustomClientFactory;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;
import ru.kontur.vostok.hercules.util.validation.StringValidators;
import ru.kontur.vostok.hercules.util.validation.Validator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Sentry client holder.
 * The class stores actual Sentry clients for event sending to the Sentry
 *
 * @author Kirill Sulim
 */
public class SentryClientHolder {

    private static final Logger LOGGER = LoggerFactory.getLogger(SentryClientHolder.class);

    private static final String DISABLE_UNCAUGHT_EXCEPTION_HANDLING = DefaultSentryClientFactory.UNCAUGHT_HANDLER_ENABLED_OPTION + "=false";
    private static final String DISABLE_IN_APP_WARN_MESSAGE = DefaultSentryClientFactory.IN_APP_FRAMES_OPTION + "=%20"; // Empty value disables warn message

    private static final String SLUG_REGEX = "[a-z0-9_\\-]+";
    private static final Validator<String> slugValidator = StringValidators.matchesWith(SLUG_REGEX);

    /**
     * Clients is a {@link AtomicReference} with a base of Sentry clients and their organizations and projects.<p>
     * The clients stores the Map with the String of organization as a key
     * and the Map as a value. <p>
     * The nested Map matching this organization contains the String of project as a key
     * and the {@link SentryClient} as a value.
     */
    private final AtomicReference<Map<String, Map<String, SentryClient>>> clients = new AtomicReference<>(Collections.emptyMap());

    private final SentryApiClient sentryApiClient;
    private final SentryClientFactory sentryClientFactory = new CustomClientFactory();
    private final String defaultTeam;

    public SentryClientHolder(Properties sinkProperties, SentryApiClient sentryApiClient) {
        this.sentryApiClient = sentryApiClient;
        this.defaultTeam = Props.DEFAULT_TEAM.extract(sinkProperties);
        update();
    }

    /**
     * Get Sentry client by pair of an organization and a project.
     * <p>
     * If the organization and the project of event exist,
     * it is the case of the cheapest and simplest operation.
     * <p>
     * If the organization or the project of event does not exist in clients,
     * the method updates clients from the Sentry
     * and then makes one another attempt to get Sentry client.
     * <p>
     * If the organization of event does not exist in the Sentry,
     * the method creates new organization and default team
     * and then makes one another attempt to get Sentry client.
     * <p>
     * If the project of event does not exist in the Sentry,
     * the method finds default team or create it, then create new project
     * and then makes one another attempt to get Sentry client.
     *
     * @param organization the organization
     * @param project the project
     * @return the {@link Optional} describing SentryClient matching an organization and a project
     */
    public Optional<SentryClient> getClient(String organization, String project) {
        Map<String, SentryClient> projectMap;
        SentryClient sentryClient = null;

        boolean triedToUpdate = false;
        boolean triedToCreateOrg = false;
        boolean triedToCreateProj = false;

        boolean success = false;
        while(!success) {
            projectMap = clients.get().get(organization);
            if (projectMap == null) {
                LOGGER.info(String.format("Cannot find organization '%s'", organization));

                Optional<String> slugError = slugValidator.validate(organization);
                if (slugError.isPresent()) {
                    LOGGER.error("Invalid organization slug (name): " + slugError.get());
                    break;
                }

                if (!triedToUpdate) {
                    LOGGER.info(String.format("Force update Sentry clients to find organization '%s'", organization));
                    update();
                    triedToUpdate = true;
                    continue;
                } else if (!triedToCreateOrg) {
                    Result<OrganizationInfo, String> orgCreationResult = sentryApiClient.createOrganization(organization);
                    if (!orgCreationResult.isOk()){
                        LOGGER.error(String.format("Cannot create organization '%s'", organization), orgCreationResult.getError());
                        break;
                    }
                    Result<TeamInfo, String> teamCreationResult = sentryApiClient.createTeam(organization, defaultTeam);
                    if (!teamCreationResult.isOk()){
                        LOGGER.error(String.format("Cannot create default team in organization '%s'", organization), teamCreationResult.getError());
                        break;
                    }
                    triedToCreateOrg = true;
                    LOGGER.info("Force update Sentry clients to pull differences from Sentry");
                    update();
                    continue;
                } else {
                    LOGGER.error(String.format("Error of creating in Sentry or updating into Sentry Sink of organization %s", organization));
                    break;
                }
            }
            sentryClient = projectMap.get(project);
            if (sentryClient == null) {
                LOGGER.info(String.format("Cannot find project '%s' in organization '%s'", project, organization));

                Optional<String> slugError = slugValidator.validate(project);
                if (slugError.isPresent()) {
                    LOGGER.error("Invalid project slug (name): " + slugError.get());
                    break;
                }

                if (!triedToUpdate) {
                    LOGGER.info(String.format("Force update Sentry clients to find project %s", project));
                    update();
                    triedToUpdate = true;
                } else if (!triedToCreateProj) {
                    Result<Void, String> checkResult = checkDefaultTeamExistenceOrCreate(organization);
                    if (!checkResult.isOk()) {
                        break;
                    }
                    Result<ProjectInfo, String> projectCreationResult = sentryApiClient.createProject(organization, defaultTeam, project);
                    if (!projectCreationResult.isOk()) {
                        LOGGER.error(String.format("Cannot create project %s", project), projectCreationResult.getError());
                    }
                    triedToCreateProj = true;
                    LOGGER.info("Force update Sentry clients to pull differences from Sentry");
                    update();
                } else {
                    LOGGER.error(String.format("Error of creating in Sentry or updating into Sentry Sink of project '%s'", project));
                    break;
                }
            } else {
                success = true;
            }
        }

        if(sentryClient == null) {
            return Optional.empty();
        }
        return Optional.of(sentryClient);
    }

    /**
     * Check default team existence in the organization.
     * If default team does not exist the method create new team.
     *
     * @param organization the organization
     * @return the {@link Result} object with success information.
     */
    private Result<Void, String> checkDefaultTeamExistenceOrCreate(String organization) {
        Result<List<TeamInfo>, String> teamListResult = sentryApiClient.getTeams(organization);
        if (!teamListResult.isOk()) {
            LOGGER.error(String.format("Cannot get teams of organization '%s' for search default team", organization), teamListResult.getError());
            return Result.error(teamListResult.getError());
        }
        for (TeamInfo teamInfo : teamListResult.get()) {
            if (teamInfo.getSlug().equals(defaultTeam)) {
                return Result.ok();
            }
        }
        Result<TeamInfo, String> creationResult = sentryApiClient.createTeam(organization, defaultTeam);
        if (!creationResult.isOk()) {
            LOGGER.error(String.format("Cannot create default team in organization '%s'", organization), creationResult.getError());
            return Result.error(teamListResult.getError());
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
    private void update() {
        try {
            Result<List<OrganizationInfo>, String> organizations = sentryApiClient.getOrganizations();
            if (!organizations.isOk()) {
                LOGGER.error("Cannot update organizations info due to: {}", organizations.getError());
                return;
            }

            Map<String, Map<String, SentryClient>> organizationMap = new HashMap<>();
            for (OrganizationInfo organizationInfo : organizations.get()) {
                String organization = organizationInfo.getSlug();

                Result<List<ProjectInfo>, String> projects = sentryApiClient.getProjects(organization);
                if (!projects.isOk()) {
                    LOGGER.error("Cannot update projects info due to: {}", projects.getError());
                    return;
                }

                Map<String, SentryClient> projectMap = new HashMap<>();
                organizationMap.put(organization, projectMap);

                for (ProjectInfo projectInfo : projects.get()) {
                    String project = projectInfo.getSlug();

                    Result<List<KeyInfo>, String> publicDsn = sentryApiClient.getPublicDsn(organization, project);
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
            LOGGER.error("Error in scheduled thread", t);
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

    private static class Props {
        static final PropertyDescription<String> DEFAULT_TEAM = PropertyDescriptions
                .stringProperty("sentry.default.team")
                .withDefaultValue("default_team")
                .withValidator(slugValidator)
                .build();
    }
}
