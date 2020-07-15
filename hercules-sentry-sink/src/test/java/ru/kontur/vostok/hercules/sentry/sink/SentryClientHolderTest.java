package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.sentry.api.model.DsnInfo;
import ru.kontur.vostok.hercules.sentry.api.model.KeyInfo;
import ru.kontur.vostok.hercules.sentry.api.model.OrganizationInfo;
import ru.kontur.vostok.hercules.sentry.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.sentry.api.model.TeamInfo;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Petr Demenev
 */
public class SentryClientHolderTest {

    private static final String MY_ORGANIZATION = "my-organization";
    private static final String MY_TEAM = MY_ORGANIZATION;
    private static final String MY_PROJECT = "my-project";
    private static final String NEW_ORGANIZATION = "new-org";
    private static final String NEW_TEAM = NEW_ORGANIZATION;
    private static final String NEW_PROJECT = "new-project";
    private static final String MY_DSN = "https://1234567813ef4c6ca4fbabc4b8f8cb7d@mysentry.io/1000001";
    private static final String NEW_DSN = "https://0234567813ef4c6ca4fbabc4b8f8cb7d@mysentry.io/1000002";

    private Map<String, SentryOrg> sentrySimulator;
    private SentryApiClient sentryApiClientMock;
    private SentryClientHolder sentryClientHolder;

    @Before
    public void init() {
        sentrySimulator = new HashMap<>();
        sentryApiClientMock = Mockito.mock(SentryApiClient.class);
        sentryClientHolder = new SentryClientHolder(sentryApiClientMock, new Properties());
    }

    @Test
    public void shouldGetClient() {
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(initTeamInfoSet(MY_TEAM), initProjectMap(MY_PROJECT)));
        getOrganizationsMock();
        getProjectsMock(MY_ORGANIZATION);
        getPublicDsnMock(MY_ORGANIZATION, MY_PROJECT);
        sentryClientHolder.update();

        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);

        Mockito.verify(sentryApiClientMock).getOrganizations();
        Mockito.verify(sentryApiClientMock, Mockito.times(0)).getProjects(NEW_ORGANIZATION);
        Mockito.verify(sentryApiClientMock, Mockito.times(0)).getPublicDsn(NEW_ORGANIZATION, NEW_PROJECT);
        Assert.assertTrue(sentryClient.isOk());
    }

    @Test
    public void shouldNotGetClientIfNameIsInvalid() {
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(initTeamInfoSet(MY_TEAM), initProjectMap(MY_PROJECT)));
        getOrganizationsMock();
        getProjectsMock(MY_ORGANIZATION);
        getPublicDsnMock(MY_ORGANIZATION, MY_PROJECT);
        sentryClientHolder.update();

        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getOrCreateClient("my_Company", MY_PROJECT);//capital letter

        Assert.assertEquals("SlugValidationError", sentryClient.getError().getType());
    }

    @Test
    public void shouldCreateNewOrganizationTeamAndProject() {
        getOrganizationsMock();
        createOrganizationMock(NEW_ORGANIZATION);
        getProjectsMock(NEW_ORGANIZATION);
        getTeamsMock(NEW_ORGANIZATION);
        createTeamMock(NEW_ORGANIZATION, NEW_ORGANIZATION);
        createProjectMock(NEW_ORGANIZATION, NEW_TEAM, NEW_PROJECT);
        getPublicDsnMock(NEW_ORGANIZATION, NEW_PROJECT);
        sentryClientHolder.update();

        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getOrCreateClient(NEW_ORGANIZATION, NEW_PROJECT);

        Mockito.verify(sentryApiClientMock, Mockito.times(2)).getOrganizations();
        Mockito.verify(sentryApiClientMock).createOrganization(NEW_ORGANIZATION);
        Mockito.verify(sentryApiClientMock).getProjects(NEW_ORGANIZATION);
        Mockito.verify(sentryApiClientMock).getTeams(NEW_ORGANIZATION);
        Mockito.verify(sentryApiClientMock).createTeam(NEW_ORGANIZATION, NEW_TEAM);
        Mockito.verify(sentryApiClientMock).createProject(NEW_ORGANIZATION, NEW_TEAM, NEW_PROJECT);
        Mockito.verify(sentryApiClientMock).getPublicDsn(NEW_ORGANIZATION, NEW_PROJECT);
        Assert.assertTrue(sentryClient.isOk());
    }

    @Test
    public void shouldCreateNewTeamAndProject() {
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(new HashSet<>(), new HashMap<>()));
        getOrganizationsMock();
        getProjectsMock(MY_ORGANIZATION);
        getTeamsMock(MY_ORGANIZATION);
        createTeamMock(MY_ORGANIZATION, MY_TEAM);
        createProjectMock(MY_ORGANIZATION, MY_TEAM, NEW_PROJECT);
        getPublicDsnMock(MY_ORGANIZATION, NEW_PROJECT);
        sentryClientHolder.update();

        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getOrCreateClient(MY_ORGANIZATION, NEW_PROJECT);

        Mockito.verify(sentryApiClientMock).getOrganizations();
        Mockito.verify(sentryApiClientMock, Mockito.times(2)).getProjects(MY_ORGANIZATION);
        Mockito.verify(sentryApiClientMock).getTeams(MY_ORGANIZATION);
        Mockito.verify(sentryApiClientMock).createTeam(MY_ORGANIZATION, MY_TEAM);
        Mockito.verify(sentryApiClientMock).createProject(MY_ORGANIZATION, MY_TEAM, NEW_PROJECT);
        Mockito.verify(sentryApiClientMock).getPublicDsn(MY_ORGANIZATION, NEW_PROJECT);
        Assert.assertTrue(sentryClient.isOk());
    }

    @Test
    public void shouldCreateNewProject() {
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(initTeamInfoSet(MY_TEAM), new HashMap<>()));
        getOrganizationsMock();
        getProjectsMock(MY_ORGANIZATION);
        getTeamsMock(MY_ORGANIZATION);
        createProjectMock(MY_ORGANIZATION, MY_TEAM, NEW_PROJECT);
        getPublicDsnMock(MY_ORGANIZATION, NEW_PROJECT);
        sentryClientHolder.update();

        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getOrCreateClient(MY_ORGANIZATION, NEW_PROJECT);

        Mockito.verify(sentryApiClientMock).getOrganizations();
        Mockito.verify(sentryApiClientMock, Mockito.times(2)).getProjects(MY_ORGANIZATION);
        Mockito.verify(sentryApiClientMock).getTeams(MY_ORGANIZATION);
        Mockito.verify(sentryApiClientMock).createProject(MY_ORGANIZATION, MY_TEAM, NEW_PROJECT);
        Mockito.verify(sentryApiClientMock).getPublicDsn(MY_ORGANIZATION, NEW_PROJECT);
        Assert.assertTrue(sentryClient.isOk());
    }

    @Test
    public void shouldPullExistingOrgFromSentryToCache() {
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(initTeamInfoSet(MY_TEAM), initProjectMap(MY_PROJECT)));
        getOrganizationsMock();
        getProjectsMock(MY_ORGANIZATION);
        getPublicDsnMock(MY_ORGANIZATION, MY_PROJECT);
        sentryClientHolder.update();
        final String otherOrganization = "other-organisation";
        sentrySimulator.put(otherOrganization, new SentryOrg(initTeamInfoSet(NEW_TEAM), initProjectMap(NEW_PROJECT)));
        getOrganizationsMock();
        getProjectsMock(otherOrganization);
        getPublicDsnMock(otherOrganization, NEW_PROJECT);

        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getOrCreateClient(otherOrganization, NEW_PROJECT);

        Mockito.verify(sentryApiClientMock, Mockito.times(2)).getOrganizations();
        Mockito.verify(sentryApiClientMock, Mockito.times(0)).createOrganization(otherOrganization);
        Assert.assertTrue(sentryClient.isOk());
    }

    @Test
    public void shouldPullExistingProjectFromSentryToCache() {
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(initTeamInfoSet(MY_TEAM), new HashMap<>()));
        getOrganizationsMock();
        getProjectsMock(MY_ORGANIZATION);
        getPublicDsnMock(MY_ORGANIZATION, MY_PROJECT);
        sentryClientHolder.update();
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(initTeamInfoSet(MY_TEAM), initProjectMap(NEW_PROJECT)));
        getProjectsMock(MY_ORGANIZATION);
        getPublicDsnMock(MY_ORGANIZATION, NEW_PROJECT);

        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getOrCreateClient(MY_ORGANIZATION, NEW_PROJECT);

        Mockito.verify(sentryApiClientMock).getOrganizations();
        Mockito.verify(sentryApiClientMock, Mockito.times(0)).createProject(MY_ORGANIZATION, MY_TEAM, NEW_PROJECT);
        Assert.assertTrue(sentryClient.isOk());
    }

    @Test
    public void shouldReturnErrorIfDsnDoesNotExist() {
        Map<String, List<KeyInfo>> projectMap = new HashMap<>();
        projectMap.put(MY_PROJECT, Collections.emptyList());
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(initTeamInfoSet(MY_TEAM), projectMap));
        getOrganizationsMock();
        getProjectsMock(MY_ORGANIZATION);
        getPublicDsnMock(MY_ORGANIZATION, MY_PROJECT);

        sentryClientHolder.update();
        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);

        Assert.assertEquals("NoActiveDSN", sentryClient.getError().getType());
    }

    @Test
    public void shouldReturnErrorIfDsnExistsButIsInactive() {
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(initTeamInfoSet(MY_TEAM), initProjectMap(MY_PROJECT)));
        sentrySimulator.get(MY_ORGANIZATION).getProjectMap().get(MY_PROJECT).get(0).setIsActive(false);
        getOrganizationsMock();
        getProjectsMock(MY_ORGANIZATION);
        getPublicDsnMock(MY_ORGANIZATION, MY_PROJECT);

        sentryClientHolder.update();
        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);

        Assert.assertEquals("NoActiveDSN", sentryClient.getError().getType());
    }

    @Test
    public void shouldReturnErrorIfClientWasRemoved() {
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(initTeamInfoSet(MY_TEAM), initProjectMap(MY_PROJECT)));
        getOrganizationsMock();
        getProjectsMock(MY_ORGANIZATION);
        getPublicDsnMock(MY_ORGANIZATION, MY_PROJECT);

        sentryClientHolder.update();
        sentryClientHolder.removeClientFromCache(MY_ORGANIZATION, MY_PROJECT);
        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);

        Assert.assertEquals("NoActiveDSN", sentryClient.getError().getType());
    }

    private Set<String> initTeamInfoSet(String initTeam) {
        Set<String> teamInfoSet = new HashSet<>();
        teamInfoSet.add(initTeam);
        return teamInfoSet;
    }

    private Map<String, List<KeyInfo>> initProjectMap(String initProject) {
        List<KeyInfo> keyList = new ArrayList<>();
        DsnInfo dsnInfo = new DsnInfo();
        dsnInfo.setPublicDsn(MY_DSN);
        KeyInfo keyInfo = new KeyInfo();
        keyInfo.setDsn(dsnInfo);
        keyInfo.setIsActive(true);
        keyList.add(keyInfo);
        Map<String, List<KeyInfo>> projectMap = new HashMap<>();
        projectMap.put(initProject, keyList);
        return projectMap;
    }

    private void getOrganizationsMock() {
        Result<List<OrganizationInfo>, ErrorInfo> result = Result.ok(
                sentrySimulator.keySet().stream()
                        .map(org -> {
                            OrganizationInfo organizationInfo = new OrganizationInfo();
                            organizationInfo.setSlug(org);
                            return organizationInfo;
                        })
                        .collect(Collectors.toList())
        );
        Mockito.when(sentryApiClientMock.getOrganizations()).thenReturn(result);
    }

    private void getProjectsMock(String organization) {
        if (organization.equals(NEW_ORGANIZATION)) {
            sentrySimulator.put(organization, new SentryOrg(new HashSet<>(), new HashMap<>()));
        }
        Result<List<ProjectInfo>, ErrorInfo> result;

        result = Result.ok(
                sentrySimulator.get(organization).getProjectMap().keySet().stream()
                        .map(proj -> {
                            ProjectInfo projectInfo = new ProjectInfo();
                            projectInfo.setSlug(proj);
                            return projectInfo;
                        })
                        .collect(Collectors.toList())
        );
        Mockito.when(sentryApiClientMock.getProjects(organization)).thenReturn(result);
    }

    private void getPublicDsnMock(String organization, String project) {
        if (organization.equals(NEW_ORGANIZATION)) {
            sentrySimulator.put(organization, new SentryOrg(new HashSet<>(), new HashMap<>()));
        }
        if (project.equals(NEW_PROJECT)) {
            List<KeyInfo> keyList = new ArrayList<>();
            DsnInfo dsnInfo = new DsnInfo();
            dsnInfo.setPublicDsn(NEW_DSN);
            KeyInfo keyInfo = new KeyInfo();
            keyInfo.setDsn(dsnInfo);
            keyInfo.setIsActive(true);
            keyList.add(keyInfo);
            sentrySimulator.get(organization).getProjectMap().put(project, keyList);
        }
        Result<List<KeyInfo>, ErrorInfo> result;

        result = Result.ok(sentrySimulator.get(organization).getProjectMap().get(project));
        Mockito.when(sentryApiClientMock.getPublicDsn(organization, project)).thenReturn(result);
    }

    private void getTeamsMock(String organization) {
        if (organization.equals(NEW_ORGANIZATION)) {
            sentrySimulator.put(organization, new SentryOrg(new HashSet<>(), new HashMap<>()));
        }
        Result<List<TeamInfo>, ErrorInfo> result;

        result = Result.ok(
                sentrySimulator.get(organization).getTeamSet().stream()
                        .map(team -> {
                            TeamInfo teamInfo = new TeamInfo();
                            teamInfo.setSlug(team);
                            return teamInfo;
                        })
                        .collect(Collectors.toList())
        );
        Mockito.when(sentryApiClientMock.getTeams(organization)).thenReturn(result);
    }

    private void createOrganizationMock(String organization) {
        Mockito.doAnswer(invocation -> {
                    sentrySimulator.put(organization, new SentryOrg(new HashSet<>(), new HashMap<>()));
                    OrganizationInfo organizationInfo = new OrganizationInfo();
                    organizationInfo.setSlug(organization);
                    return Result.ok(organizationInfo);
                }
        ).when(sentryApiClientMock).createOrganization(organization);
    }

    private void createProjectMock(String organization, String team, String project) {
        if (organization.equals(NEW_ORGANIZATION)) {
            sentrySimulator.put(organization, new SentryOrg(new HashSet<>(), new HashMap<>()));
            sentrySimulator.get(organization).getTeamSet().add(team);
        }
        Mockito.doAnswer(invocation -> {
                    List<KeyInfo> dsnList = new ArrayList<>();
                    DsnInfo dsnInfo = new DsnInfo();
                    dsnInfo.setPublicDsn(NEW_DSN);
                    KeyInfo keyInfo = new KeyInfo();
                    keyInfo.setDsn(dsnInfo);
                    keyInfo.setIsActive(true);
                    dsnList.add(keyInfo);
                    sentrySimulator.get(organization).getProjectMap().put(project, dsnList);
                    ProjectInfo projectInfo = new ProjectInfo();
                    projectInfo.setSlug(project);
                    return Result.ok(projectInfo);
                }
        ).when(sentryApiClientMock).createProject(organization, team, project);
    }

    private void createTeamMock(String organization, String team) {
        if (organization.equals(NEW_ORGANIZATION)) {
            sentrySimulator.put(organization, new SentryOrg(new HashSet<>(), new HashMap<>()));
        }
        Mockito.doAnswer(invocation -> {
                    sentrySimulator.get(organization).getTeamSet().add(team);
                    TeamInfo teamInfo = new TeamInfo();
                    teamInfo.setSlug(team);
                    return Result.ok(teamInfo);
                }
        ).when(sentryApiClientMock).createTeam(organization, team);
    }

    private class SentryOrg {
        private Set<String> teamSet;
        private Map<String, List<KeyInfo>> projectMap;

        private SentryOrg(Set<String> teamSet, Map<String, List<KeyInfo>> projectMap) {
            this.teamSet = teamSet;
            this.projectMap = projectMap;
        }

        private Set<String> getTeamSet() {
            return teamSet;
        }

        private Map<String, List<KeyInfo>> getProjectMap() {
            return projectMap;
        }
    }
}
