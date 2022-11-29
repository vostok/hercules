package ru.kontur.vostok.hercules.sentry.client.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.sentry.client.ErrorInfo;
import ru.kontur.vostok.hercules.sentry.client.api.SentryApiClient;
import ru.kontur.vostok.hercules.sentry.client.api.model.OrganizationInfo;
import ru.kontur.vostok.hercules.sentry.client.api.model.ProjectInfo;
import ru.kontur.vostok.hercules.sentry.client.api.model.TeamInfo;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.DsnFetcherClient;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Dsn;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;

/**
 * @author Tatyana Tokmyanina
 */
public class SentryConnectorHolderTest {
    private static final Dsn dsn = new Dsn("public", "id", null);
    private static final SentryDestination goodDestination = SentryDestination.of("org", "project");
    private static OrganizationInfo orgInfo;
    private static ProjectInfo projectInfo;
    private static TeamInfo teamInfo;

    @Before
    public void init() {
        orgInfo = new OrganizationInfo();
        orgInfo.setSlug("org");
        projectInfo = new ProjectInfo();
        projectInfo.setSlug("project");
        teamInfo = new TeamInfo();
        teamInfo.setSlug("org");
    }

    @Test
    public void shouldGetConnectorIfExist()
            throws IOException, InterruptedException {
        SentryApiClient sentryApiClient = mock(SentryApiClient.class);
        when(sentryApiClient.getOrganizations()).thenReturn(Result.ok(List.of(orgInfo)));
        when(sentryApiClient.getProjects(anyString())).thenReturn(Result.ok(List.of(projectInfo)));
        DsnFetcherClient dsnFetcherClient = mock(DsnFetcherClient.class);
        when(dsnFetcherClient.fetchDsn(any())).thenReturn(dsn);
        SentryConnectorHolderImpl sentryConnectorHolder = new SentryConnectorHolderImpl(
                sentryApiClient,
                600000,
    dsnFetcherClient
        );
        Result<Dsn, ErrorInfo> result = sentryConnectorHolder.getOrCreateConnector(goodDestination);
        Assert.assertTrue(result.isOk());
        Assert.assertEquals(dsn, result.get());
        verify(dsnFetcherClient).fetchDsn(any());
        result = sentryConnectorHolder.getOrCreateConnector(goodDestination);
        Assert.assertTrue(result.isOk());
        Assert.assertEquals(dsn, result.get());
        verify(dsnFetcherClient).fetchDsn(any());
    }

    @Test
    public void shouldReturnEmptyConnectorIfDsnIsUnavailable()
            throws IOException, InterruptedException {
        SentryApiClient sentryApiClient = mock(SentryApiClient.class);
        when(sentryApiClient.getOrganizations()).thenReturn(Result.ok(List.of(orgInfo)));
        when(sentryApiClient.getProjects(anyString())).thenReturn(Result.ok(List.of(projectInfo)));
        DsnFetcherClient dsnFetcherClient = mock(DsnFetcherClient.class);
        when(dsnFetcherClient.fetchDsn(any())).thenReturn(Dsn.unavailable());
        SentryConnectorHolderImpl sentryConnectorHolder = new SentryConnectorHolderImpl(
                sentryApiClient,
                600000,
                dsnFetcherClient
        );
        Result<Dsn, ErrorInfo> result = sentryConnectorHolder.getOrCreateConnector(goodDestination);
        Assert.assertTrue(result.isOk());
        Assert.assertEquals(Dsn.unavailable(), result.get());
        verify(dsnFetcherClient).fetchDsn(any());
        result = sentryConnectorHolder.getOrCreateConnector(goodDestination);
        Assert.assertTrue(result.isOk());
        Assert.assertEquals(Dsn.unavailable(), result.get());
        verify(dsnFetcherClient).fetchDsn(any());
    }

    @Test
    public void shouldCreateProjectIfNotExist() throws IOException, InterruptedException {
        SentryApiClient sentryApiClient = mock(SentryApiClient.class);
        when(sentryApiClient.getOrganizations()).thenReturn(Result.ok(List.of(orgInfo)));
        when(sentryApiClient.getProjects(anyString())).thenReturn(Result.ok(new ArrayList<>()));
        when(sentryApiClient.getTeams(any())).thenReturn(Result.ok(new ArrayList<>()));
        when(sentryApiClient.createTeam(any(), any())).thenReturn(Result.ok(teamInfo));
        when(sentryApiClient.createProject(any(), any(), any())).thenReturn(Result.ok(projectInfo));
        DsnFetcherClient dsnFetcherClient = mock(DsnFetcherClient.class);
        when(dsnFetcherClient.fetchDsn(any())).thenReturn(dsn);
        SentryConnectorHolderImpl sentryConnectorHolder = new SentryConnectorHolderImpl(
                sentryApiClient,
                600000,
                dsnFetcherClient
        );
        Result<Dsn, ErrorInfo> result = sentryConnectorHolder.getOrCreateConnector(goodDestination);
        Assert.assertTrue(result.isOk());
        Assert.assertEquals(dsn, result.get());
        verify(sentryApiClient).createTeam(any(), any());
        verify(sentryApiClient).createProject(any(), any(), any());
    }

    @Test
    public void shouldReturnEmptyDsnIfOrganizationNotExist()
            throws IOException, InterruptedException {
        SentryApiClient sentryApiClient = mock(SentryApiClient.class);
        when(sentryApiClient.getOrganizations()).thenReturn(Result.ok(new ArrayList<>()));
        DsnFetcherClient dsnFetcherClient = mock(DsnFetcherClient.class);
        when(dsnFetcherClient.fetchDsn(any())).thenReturn(dsn);
        SentryConnectorHolderImpl sentryConnectorHolder = new SentryConnectorHolderImpl(
                sentryApiClient,
                600000,
                dsnFetcherClient
        );
        Result<Dsn, ErrorInfo> result = sentryConnectorHolder.getOrCreateConnector(goodDestination);
        Assert.assertTrue(result.isOk());
        Assert.assertEquals(Dsn.unavailable(), result.get());
    }
}
