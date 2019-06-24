package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.sentry.api.SentryApiClient;
import ru.kontur.vostok.hercules.sentry.api.model.DsnInfo;
import ru.kontur.vostok.hercules.sentry.api.model.KeyInfo;
import ru.kontur.vostok.hercules.sentry.api.model.OrganizationInfo;
import ru.kontur.vostok.hercules.sentry.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.sentry.api.model.TeamInfo;
import ru.kontur.vostok.hercules.util.functional.Result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Petr Demenev
 */
public class SentryClientHolderTest {

    private static final String MY_ORGANIZATION = "my-organization";
    private static final String MY_TEAM = MY_ORGANIZATION;
    private static final String EXISTING_ORGANIZATION = "existing-organization";
    private static final String MY_PROJECT = "my-project";
    private static final String EXISTING_PROJECT = "existing-project";
    private static final String NEW_PROJECT = "new-project";
    private static final String MY_DSN = "https://1234567813ef4c6ca4fbabc4b8f8cb7d@mysentry.io/1000001";
    private static final String NEW_DSN = "https://0234567813ef4c6ca4fbabc4b8f8cb7d@mysentry.io/1000002";

    private Map<String, SentryOrg> sentrySimulator;
    private SentryApiClient sentryApiClientMock;

    @Before
    public void init() {
        List<String> keyList = new ArrayList<>();
        keyList.add(MY_DSN);
        Set<String> teamInfoSet = new HashSet<>();
        teamInfoSet.add(MY_TEAM);
        Map<String, List<String>> projectMap = new HashMap<>();
        projectMap.put(MY_PROJECT, keyList);

        sentrySimulator = new HashMap<>();
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(teamInfoSet, projectMap));

        sentryApiClientMock = mock(SentryApiClient.class);
        getOrganizationsMock();
        getProjectsMock(MY_ORGANIZATION);
        getPublicDsnMock(MY_ORGANIZATION, MY_PROJECT);
    }

    @Test
    public void shouldGetClient() {
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        sentryClientHolder.update();
        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getClient(MY_ORGANIZATION, MY_PROJECT);

        verify(sentryApiClientMock).getOrganizations();
        verify(sentryApiClientMock).getProjects(MY_ORGANIZATION);
        verify(sentryApiClientMock).getPublicDsn(MY_ORGANIZATION, MY_PROJECT);
        assertTrue(sentryClient.isOk());
        assertNotNull(sentryClient.get());
    }

    @Test
    public void shouldNotGetClient() {
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        sentryClientHolder.update();
        Result<SentryClient, ErrorInfo> sentryClient = sentryClientHolder.getClient(MY_ORGANIZATION, "other-project");

        verify(sentryApiClientMock).getOrganizations();
        verify(sentryApiClientMock).getProjects(MY_ORGANIZATION);
        verify(sentryApiClientMock).getPublicDsn(MY_ORGANIZATION, MY_PROJECT);
        assertFalse(sentryClient.isOk());
        assertNull(sentryClient.get());
    }

    @Test
    public void shouldAdmitGoodName() {
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        Result<Void, ErrorInfo> result = sentryClientHolder.validateSlugs(MY_ORGANIZATION, MY_PROJECT);

        assertTrue(result.isOk());
    }

    @Test
    public void shouldIndicateInvalidName() {
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        Result<Void, ErrorInfo> result = sentryClientHolder.validateSlugs("my_Company", MY_PROJECT); //capital letter

        assertFalse(result.isOk());
    }

    @Test
    public void shouldFindExistingOrganization() {
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        sentryClientHolder.update();
        Result<Void, ErrorInfo> result = sentryClientHolder.createOrganizationIfNotExists(MY_ORGANIZATION);

        verify(sentryApiClientMock, times(2)).getOrganizations();
        assertTrue(result.isOk());
    }

    @Test
    public void shouldCreateNewOrganization() {
        String newOrg = "new-org";
        createOrganizationMock(newOrg);
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        sentryClientHolder.update();
        Result<Void, ErrorInfo> result = sentryClientHolder.createOrganizationIfNotExists(newOrg);

        verify(sentryApiClientMock, times(2)).getOrganizations();
        verify(sentryApiClientMock).createOrganization(newOrg);
        assertTrue(result.isOk());
    }

    @Test
    public void shouldReturnErrorOfOrgCreation() {
        createOrganizationMock(EXISTING_ORGANIZATION);
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        sentryClientHolder.update();
        Result<Void, ErrorInfo> result = sentryClientHolder.createOrganizationIfNotExists(EXISTING_ORGANIZATION);

        verify(sentryApiClientMock, times(2)).getOrganizations();
        verify(sentryApiClientMock).createOrganization(EXISTING_ORGANIZATION);
        assertEquals("CONFLICT", result.getError());
    }

    @Test
    public void shouldFindExistingProject() {
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        sentryClientHolder.update();
        Result<Void, ErrorInfo> result = sentryClientHolder.createProjectIfNotExists(MY_ORGANIZATION, MY_PROJECT);

        verify(sentryApiClientMock, times(2)).getProjects(MY_ORGANIZATION);
        assertTrue(result.isOk());
    }

    @Test
    public void shouldCreateNewProject() {
        createProjectMock(MY_ORGANIZATION, NEW_PROJECT);
        getTeamsMock(MY_ORGANIZATION);
        getPublicDsnMock(MY_ORGANIZATION, NEW_PROJECT);
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        sentryClientHolder.update();
        Result<Void, ErrorInfo> result = sentryClientHolder.createProjectIfNotExists(MY_ORGANIZATION, NEW_PROJECT);

        verify(sentryApiClientMock, times(2)).getProjects(MY_ORGANIZATION);
        verify(sentryApiClientMock).getTeams(MY_ORGANIZATION);
        verify(sentryApiClientMock).createProject(MY_ORGANIZATION, MY_TEAM, NEW_PROJECT);
        assertTrue(result.isOk());
    }

    @Test
    public void shouldReturnErrorOfProjectCreation() {
        createProjectMock(MY_ORGANIZATION, EXISTING_PROJECT);
        getTeamsMock(MY_ORGANIZATION);
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        sentryClientHolder.update();
        Result<Void, ErrorInfo> result = sentryClientHolder.createProjectIfNotExists(MY_ORGANIZATION, EXISTING_PROJECT);

        verify(sentryApiClientMock, times(2)).getProjects(MY_ORGANIZATION);
        verify(sentryApiClientMock).getTeams(MY_ORGANIZATION);
        verify(sentryApiClientMock).createProject(MY_ORGANIZATION, MY_TEAM, EXISTING_PROJECT);
        assertEquals("CONFLICT", result.getError());
    }

    @Test
    public void shouldFindExistingTeam() {
        getTeamsMock(MY_ORGANIZATION);
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        sentryClientHolder.update();
        Result<Void, ErrorInfo> result = sentryClientHolder.createDefaultTeamIfNotExists(MY_ORGANIZATION);

        verify(sentryApiClientMock).getTeams(MY_ORGANIZATION);
        assertTrue(result.isOk());
    }

    @Test
    public void shouldCreateTeam() {
        initWithoutTeams();
        getTeamsMock(MY_ORGANIZATION);
        createTeamMock(MY_ORGANIZATION, MY_TEAM);
        SentryClientHolder sentryClientHolder = new SentryClientHolder(sentryApiClientMock);

        sentryClientHolder.update();
        Result<Void, ErrorInfo> result = sentryClientHolder.createDefaultTeamIfNotExists(MY_ORGANIZATION);

        verify(sentryApiClientMock).getTeams(MY_ORGANIZATION);
        verify(sentryApiClientMock).createTeam(MY_ORGANIZATION, MY_TEAM);
        assertTrue(result.isOk());
    }

    private void initWithoutTeams() {
        List<String> keyList = new ArrayList<>();
        keyList.add(MY_DSN);
        Set<String> teamInfoSet = new HashSet<>();
        Map<String, List<String>> projectMap = new HashMap<>();
        projectMap.put(MY_PROJECT, keyList);

        sentrySimulator = new HashMap<>();
        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(teamInfoSet, projectMap));

        sentryApiClientMock = mock(SentryApiClient.class);
        getOrganizationsMock();
        getProjectsMock(MY_ORGANIZATION);
        getPublicDsnMock(MY_ORGANIZATION, MY_PROJECT);
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
        when(sentryApiClientMock.getOrganizations()).thenReturn(result);
    }

    private void getProjectsMock(String organization) {
        Result<List<ProjectInfo>, ErrorInfo> result;
        try {
            result = Result.ok(
                    sentrySimulator.get(organization).getProjectMap().keySet().stream()
                            .map(proj -> {
                                ProjectInfo projectInfo = new ProjectInfo();
                                projectInfo.setSlug(proj);
                                return projectInfo;
                            })
                            .collect(Collectors.toList())
            );
        } catch (NullPointerException e) {
            result = Result.error(new ErrorInfo("NOT_FOUND", 404));
        }
        when(sentryApiClientMock.getProjects(organization)).thenReturn(result);
    }

    private void getPublicDsnMock(String organization, String project) {
        Result<List<KeyInfo>, ErrorInfo> result;
        if (project.equals(NEW_PROJECT)) {
            List<String> keyList = new ArrayList<>();
            keyList.add(NEW_DSN);
            sentrySimulator.get(organization).getProjectMap().put(project, keyList);
        }
        try {
            result = Result.ok(
                    sentrySimulator.get(organization).getProjectMap().get(project).stream()
                            .map(dsn -> {
                                DsnInfo dsnInfo = new DsnInfo();
                                dsnInfo.setPublicDsn(dsn);
                                KeyInfo keyInfo = new KeyInfo();
                                keyInfo.setDsn(dsnInfo);
                                return keyInfo;
                            })
                            .collect(Collectors.toList())
            );
        } catch (NullPointerException e) {
            result = Result.error(new ErrorInfo("NOT_FOUND", 404));
        }
        when(sentryApiClientMock.getPublicDsn(organization, project)).thenReturn(result);
    }

    private void getTeamsMock(String organization) {
        Result<List<TeamInfo>, ErrorInfo> result;
        try {
            result = Result.ok(
                    sentrySimulator.get(organization).getTeamSet().stream()
                            .map(team -> {
                                TeamInfo teamInfo = new TeamInfo();
                                teamInfo.setSlug(team);
                                return teamInfo;
                            })
                            .collect(Collectors.toList())
            );
        } catch (NullPointerException e) {
            result = Result.error(new ErrorInfo("NOT_FOUND", 404));
        }
        when(sentryApiClientMock.getTeams(organization)).thenReturn(result);
    }

    private void createOrganizationMock(String organization) {
        doAnswer(invocation -> {
            if (organization.equals(EXISTING_ORGANIZATION)) {
                return Result.error("CONFLICT");
            } else {
                sentrySimulator.put(organization, new SentryOrg(new HashSet<>(), new HashMap<>()));
                OrganizationInfo organizationInfo = new OrganizationInfo();
                organizationInfo.setSlug(organization);
                return Result.ok(organizationInfo);
            }
        }
        ).when(sentryApiClientMock).createOrganization(organization);
    }

    private void createProjectMock(String organization, String project) {
        doAnswer(invocation -> {
            if (project.equals(EXISTING_PROJECT)) {
                return Result.error("CONFLICT");
            } else {
                List<String> dsnList = new ArrayList<>();
                dsnList.add("https://1234567813ef4c6ca4fbabc4b8f8cb7e@mysentry.io/1000002");
                sentrySimulator.get(organization).getProjectMap().put(project, dsnList);
                ProjectInfo projectInfo = new ProjectInfo();
                projectInfo.setSlug(project);
                return Result.ok(projectInfo);
            }
        }
        ).when(sentryApiClientMock).createProject(organization, MY_TEAM, project);
    }

    private void createTeamMock(String organization, String team) {
        doAnswer(invocation -> {
            sentrySimulator.get(organization).getTeamSet().add(team);
            TeamInfo teamInfo = new TeamInfo();
            teamInfo.setSlug(team);
            return Result.ok(teamInfo);
        }
        ).when(sentryApiClientMock).createTeam(organization, team);
    }

    private class SentryOrg {
        private Set<String> teamSet;
        private Map<String, List<String>> projectMap;

        private SentryOrg(Set<String> teamSet, Map<String, List<String>> projectMap) {
            this.teamSet = teamSet;
            this.projectMap = projectMap;
        }

        private Set<String> getTeamSet() {
            return teamSet;
        }

        private Map<String, List<String>> getProjectMap() {
            return projectMap;
        }
    }
}
