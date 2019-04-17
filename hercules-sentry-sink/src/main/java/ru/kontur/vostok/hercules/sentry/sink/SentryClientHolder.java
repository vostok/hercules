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
import ru.kontur.vostok.hercules.util.functional.Result;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    private static final String DEFAULT_TEAM = "default_team";

    /**
     * Clients is a {@link AtomicReference} with a base of Sentry clients and their organisations and projects.<p>
     * The clients stores the Map with the String of organisation as a key
     * and the Map as a value. <p>
     * The nested Map matching this organisation contains the String of project as a key
     * and the {@link SentryClient} as a value.
     */
    private final AtomicReference<Map<String, Map<String, SentryClient>>> clients = new AtomicReference<>(Collections.emptyMap());
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(1);

    private final SentryApiClient sentryApiClient;
    private final SentryClientFactory sentryClientFactory = new CustomClientFactory();

    public SentryClientHolder(SentryApiClient sentryApiClient) {
        this.sentryApiClient = sentryApiClient;
        this.scheduledExecutor.scheduleAtFixedRate(this::update, 0,10000, TimeUnit.MILLISECONDS);
    }

    /**
     * Get Sentry client by pair of an organisation and a project.
     * <p>
     * If the organisation and the project of event exist,
     * it is the case of the cheapest and simplest operation.
     * <p>
     * If the organisation or the project of event does not exist in clients,
     * the method updates clients from the Sentry
     * and then makes one another attempt to get Sentry client.
     * <p>
     * If the organisation of event does not exist in the Sentry,
     * the method creates new organisation and default team
     * and then makes one another attempt to get Sentry client.
     * <p>
     * If the project of event does not exist in the Sentry,
     * the method finds default team or create it, then create new project
     * and then makes one another attempt to get Sentry client.
     *
     * @param organisation the organisation
     * @param project the project
     * @return the {@link Optional} describing SentryClient matching an organisation and a project
     */
    public Optional<SentryClient> getClient(String organisation, String project) {
        Map<String, SentryClient> projectMap;
        SentryClient sentryClient;
        boolean triedToUpdate = false;
        boolean triedToCreateOrg = false;
        boolean triedToCreateProj = false;
        while(true) {
            projectMap = clients.get().get(organisation);
            if (projectMap == null) {
                //TODO add org validation
                LOGGER.info(String.format("Cannot find organization '%s'", organisation));
                if (!triedToUpdate) {
                    LOGGER.info(String.format("Force update Sentry clients to find organization '%s'", organisation));
                    update();
                    triedToUpdate = true;
                    continue;
                } else if (!triedToCreateOrg) {
                    Result<OrganizationInfo, String> orgCreationResult = sentryApiClient.createOrganization(organisation);
                    if (!orgCreationResult.isOk()){
                        LOGGER.error(String.format("Cannot create organization '%s'", organisation), orgCreationResult.getError());
                        return Optional.empty();
                    }
                    Result<TeamInfo, String> teamCreationResult = sentryApiClient.createTeam(organisation, DEFAULT_TEAM);
                    if (!teamCreationResult.isOk()){
                        LOGGER.error(String.format("Cannot create default team in organisation '%s'", organisation), teamCreationResult.getError());
                        return Optional.empty();
                    }
                    triedToCreateOrg = true;
                    LOGGER.info("Force update Sentry clients to pull differences from Sentry");
                    update();
                    continue;
                } else {
                    LOGGER.error(String.format("Error of creating in Sentry or updating into Sentry Sink of organisation %s", organisation));
                    return Optional.empty();
                }
            }
            sentryClient = projectMap.get(project);
            if (sentryClient == null) {
                //TODO add project validation
                LOGGER.info(String.format("Cannot find project '%s' in organisation '%s'", project, organisation));
                if (!triedToUpdate) {
                    LOGGER.info(String.format("Force update Sentry clients to find project %s", project));
                    update();
                    triedToUpdate = true;
                } else if (!triedToCreateProj) {
                    Result<Void, String> checkResult = checkDefaultTeamExistenceOrCreate(organisation);
                    if (!checkResult.isOk()) {
                        return Optional.empty();
                    }
                    Result<ProjectInfo, String> projectCreationResult = sentryApiClient.createProject(organisation, DEFAULT_TEAM, project);
                    if (!projectCreationResult.isOk()) {
                        LOGGER.error(String.format("Cannot create project %s", project), projectCreationResult.getError());
                    }
                    triedToCreateProj = true;
                    LOGGER.info("Force update Sentry clients to pull differences from Sentry");
                    update();
                } else {
                    LOGGER.error(String.format("Error of creating in Sentry or updating into Sentry Sink of project '%s'", project));
                    return Optional.empty();
                }
            } else {
                break;
            }
        }
        return Optional.of(sentryClient);
    }

    /**
     * Check default team existence in the organisation.
     * If default team does not exist the method create new team.
     *
     * @param organisation the organisation
     * @return the {@link Result} object with success information.
     */
    private Result<Void, String> checkDefaultTeamExistenceOrCreate(String organisation) {
        Result<List<TeamInfo>, String> teamListResult = sentryApiClient.getTeams(organisation);
        if (!teamListResult.isOk()) {
            LOGGER.error(String.format("Cannot get teams of organisation '%s' for search default team", organisation), teamListResult.getError());
            return Result.error(teamListResult.getError());
        }
        for (TeamInfo teamInfo : teamListResult.get()) {
            if (teamInfo.getSlug().equals(DEFAULT_TEAM)) {
                return Result.ok();
            }
        }
        Result<TeamInfo, String> creationResult = sentryApiClient.createTeam(organisation, DEFAULT_TEAM);
        if (!creationResult.isOk()) {
            LOGGER.error(String.format("Cannot create default team in organisation '%s'", organisation), creationResult.getError());
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
     * <p>
     * This method executes by schedule
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

                Result<List<ProjectInfo>, String> projects = sentryApiClient.getProjects(organizationInfo.getSlug());
                if (!projects.isOk()) {
                    LOGGER.error("Cannot update projects info due to: {}", projects.getError());
                    return;
                }

                Map<String, SentryClient> projectMap = new HashMap<>();
                organizationMap.put(organizationInfo.getSlug(), projectMap);

                for (ProjectInfo projectInfo : projects.get()) {

                    Result<List<KeyInfo>, String> publicDsn = sentryApiClient.getPublicDsn(organizationInfo.getSlug(), projectInfo.getSlug());
                    if (!publicDsn.isOk()) {
                        LOGGER.error("Cannot get public dsn for project '{}' due to: {}", projectInfo.getSlug(), publicDsn.getError());
                        return;
                    }
                    Optional<String> dsn = publicDsn.get().stream()
                            .findAny()
                            .map(KeyInfo::getDsn)
                            .map(DsnInfo::getPublicDsn);
                    String dsnString = "";
                    if (dsn.isPresent()) {
                        dsnString = dsn.get();
                        try {
                            new URL(dsnString);
                        } catch (MalformedURLException e) {
                            throw new Exception(String.format("Malformed dsn '%s', there might be an error in sentry configuration", dsnString));
                        }
                    }

                    projectMap.put(projectInfo.getSlug(), SentryClientFactory.sentryClient(applySettings(dsnString), sentryClientFactory));
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
}
