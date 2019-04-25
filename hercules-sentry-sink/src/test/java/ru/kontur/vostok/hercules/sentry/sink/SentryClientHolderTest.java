package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
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
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SentryClientHolderTest {

    private static final String DEFAULT_TEAM = "default_team";
    private static final String MY_ORGANIZATION = "my-organization";
    private static final String MY_PROJECT = "my-project";
    private static final String MY_DSN = "https://1234567813ef4c6ca4fbabc4b8f8cb7d@mysentry.io/1000001";

    private static Map<String, SentryOrg> sentrySimulator = new HashMap<>();

    @Before
    public void init() {
        List<String> keyList = new ArrayList<>();
        keyList.add(MY_DSN);
        Set<String> teamInfoSet = new HashSet<>();
        teamInfoSet.add(DEFAULT_TEAM);
        Map<String, List<String>> projectMap = new HashMap<>();
        projectMap.put(MY_PROJECT, keyList);

        sentrySimulator.put(MY_ORGANIZATION, new SentryOrg(teamInfoSet, projectMap));
    }

    @Test
    public void shouldGetClient() {
        SentryApiClient sentryApiClientMock = mock(SentryApiClient.class);
        getOrganizationsMock(sentryApiClientMock);
        getProjectsMock(sentryApiClientMock, MY_ORGANIZATION);
        getPublicDsnMock(sentryApiClientMock, MY_ORGANIZATION, MY_PROJECT);

        SentryClientHolder sentryClientHolder = new SentryClientHolder(new Properties(), sentryApiClientMock);
        Optional<SentryClient> sentryClient = sentryClientHolder.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);

        assertTrue(sentryClient.isPresent());
    }

    private void getOrganizationsMock(SentryApiClient sentryApiClientMock) {
        Result<List<OrganizationInfo>, String> result = Result.ok(
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

    private void getProjectsMock(SentryApiClient sentryApiClientMock,
                                 String organization) {
        Result<List<ProjectInfo>, String> result = Result.ok(
                sentrySimulator.get(organization).getProjectMap().keySet().stream()
                        .map(proj -> {
                            ProjectInfo projectInfo = new ProjectInfo();
                            projectInfo.setSlug(proj);
                            return projectInfo;
                        })
                        .collect(Collectors.toList())
        );
        when(sentryApiClientMock.getProjects(organization)).thenReturn(result);
    }

    private void getPublicDsnMock(SentryApiClient sentryApiClientMock,
                                  String organization,
                                  String project) {
        Result<List<KeyInfo>, String> result = Result.ok(
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
        when(sentryApiClientMock.getPublicDsn(organization, project)).thenReturn(result);
    }

    private void getTeamsMock(SentryApiClient sentryApiClientMock,
                              String organization) {
        Result<List<TeamInfo>, String> result = Result.ok(
                sentrySimulator.get(organization).getTeamSet().stream()
                        .map(team -> {
                            TeamInfo teamInfo = new TeamInfo();
                            teamInfo.setSlug(team);
                            return teamInfo;
                        })
                        .collect(Collectors.toList())
        );
        when(sentryApiClientMock.getTeams(organization)).thenReturn(result);
    }

    private void createOrganizationMock(SentryApiClient sentryApiClientMock,
                                        String organization) {
        doAnswer(new Answer() {
                     @Override
                     public Result<OrganizationInfo, String> answer(InvocationOnMock invocation) throws Throwable {
                         sentrySimulator.put(organization, new SentryOrg(new HashSet<>(), new HashMap<>()));
                         OrganizationInfo organizationInfo = new OrganizationInfo();
                         organizationInfo.setSlug(organization);
                         return Result.ok(organizationInfo);
                     }
                 }
        ).when(sentryApiClientMock).createOrganization(organization);
    }

    private void createProjectMock(SentryApiClient sentryApiClientMock,
                                   String organization,
                                   String project,
                                   String dsn) {
        doAnswer(new Answer() {
                     @Override
                     public Result<ProjectInfo, String> answer(InvocationOnMock invocation) throws Throwable {
                         List<String> dsnList = new ArrayList<>();
                         dsnList.add(dsn);
                         sentrySimulator.get(organization).getProjectMap().put(project, dsnList);
                         ProjectInfo projectInfo = new ProjectInfo();
                         projectInfo.setSlug(project);
                         return Result.ok(projectInfo);
                     }
                 }
        ).when(sentryApiClientMock).createProject(organization, DEFAULT_TEAM, project);
    }

    private void createTeamMock(SentryApiClient sentryApiClientMock,
                                String organization,
                                String team) {
        doAnswer(new Answer() {
                     @Override
                     public Result<TeamInfo, String> answer(InvocationOnMock invocation) throws Throwable {
                         sentrySimulator.get(organization).getTeamSet().add(team);
                         TeamInfo teamInfo = new TeamInfo();
                         teamInfo.setSlug(team);
                         return Result.ok(teamInfo);
                     }
                 }
        ).when(sentryApiClientMock).createTeam(organization, team);
    }

    private class SentryOrg {
        private Set<String> teamSet;
        private Map<String, List<String>> projectMap;

        public SentryOrg(Set<String> teamSet, Map<String, List<String>> projectMap) {
            this.teamSet = teamSet;
            this.projectMap = projectMap;
        }

        public Set<String> getTeamSet() {
            return teamSet;
        }

        public Map<String, List<String>> getProjectMap() {
            return projectMap;
        }
    }
}
