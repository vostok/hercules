package ru.kontur.vostok.hercules.sentry.sink.client;

import io.sentry.connection.ConnectionException;
import io.sentry.connection.TooManyRequestsException;
import io.sentry.dsn.InvalidDsnException;
import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.sentry.client.ErrorInfo;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.routing.Router;
import ru.kontur.vostok.hercules.sentry.client.impl.v9.connector.HerculesSentryClient;
import ru.kontur.vostok.hercules.sentry.client.impl.v9.connector.SentryConnectorHolderImplV9;
import ru.kontur.vostok.hercules.sentry.client.impl.v9.connector.SentryConnectorImplV9;
import ru.kontur.vostok.hercules.sentry.sink.SentrySyncProcessor;
import ru.kontur.vostok.hercules.util.routing.SentryDestination;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Petr Demenev
 */
public class SentrySyncProcessorTest {

    @SuppressWarnings("unchecked")
    private final Router<Event, SentryDestination> router = mock(Router.class);

    private static final UUID someUuid = UUID.randomUUID();

    private static SentryConnectorHolderImplV9 sentryConnectorHolderMock;
    private static SentryConnectorImplV9 sentryConnectorMock;
    private static HerculesSentryClient herculesSentryClientMock;
    private static final MetricsCollector metricsCollectorMock = mock(MetricsCollector.class);
    private static SentrySyncProcessor sentrySyncProcessor;
    private static final String MY_PROJECT = "my-project";
    private static final String MY_ORGANIZATION = "my-organization";
    private static final String MY_ENVIRONMENT = "test";
    private static final int RETRY_COUNT = 3;
    private static final Event EVENT = createEvent();
    private static final String CLIENT_API_ERROR = "ClientApiError";

    /**
     * Mock metrics
     */
    @Before
    public void init() {
        sentryConnectorMock = mock(SentryConnectorImplV9.class);
        sentryConnectorHolderMock = mock(SentryConnectorHolderImplV9.class);
        herculesSentryClientMock = mock(HerculesSentryClient.class);
        sentrySyncProcessor = new SentrySyncProcessor(
                new Properties(),
                sentryConnectorHolderMock,
                metricsCollectorMock,
                router,
                "",
                "");
        when(metricsCollectorMock.meter(anyString())).thenReturn(n -> {
        });
        when(metricsCollectorMock.timer(anyString())).thenReturn((duration, unit) -> {
        });
        when(router.route(EVENT)).thenReturn(SentryDestination.of(MY_ORGANIZATION, MY_PROJECT));
    }

    @Test
    public void shouldReturnFalseRouterReturnsNowhereDestination() throws BackendServiceFailedException {
        when(router.route(EVENT)).thenReturn(SentryDestination.toNowhere());

        assertFalse(sentrySyncProcessor.process(EVENT));
    }

    @Test(expected = BackendServiceFailedException.class)
    public void shouldThrowExceptionWhenHappensRetryableErrorOfApiClient() throws BackendServiceFailedException {
        when(sentryConnectorHolderMock.getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 404)));

        sentrySyncProcessor.process(EVENT);
    }

    @Test
    public void shouldRetryWhenHappensRetryableErrorOfApiClient() {
        sentryConnectorMock = mock(SentryConnectorImplV9.class);
        sentryConnectorHolderMock = mock(SentryConnectorHolderImplV9.class);
        herculesSentryClientMock = mock(HerculesSentryClient.class);
        sentrySyncProcessor = new SentrySyncProcessor(
                new Properties(),
                sentryConnectorHolderMock,
                metricsCollectorMock,
                router,
                "",
                "");
        when(sentryConnectorHolderMock.getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 404)));

        try {
            sentrySyncProcessor.process(EVENT);
        } catch (BackendServiceFailedException e) {

            verify(sentryConnectorHolderMock, times(RETRY_COUNT + 1)).getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT);
        }
    }

    @Test
    public void shouldReturnFalseWhenHappensNonRetryableErrorOfApiClient() throws BackendServiceFailedException {
        when(sentryConnectorHolderMock.getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 400)));
        when(sentryConnectorMock.getSentryClient()).thenReturn(herculesSentryClientMock);
        doNothing().when(herculesSentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        boolean result = sentrySyncProcessor.process(EVENT);

        assertFalse(result);
    }

    @Test
    public void shouldNotRetryWhenHappensNonRetryableErrorOfApiClient() throws BackendServiceFailedException {
        sentryConnectorHolderMock = mock(SentryConnectorHolderImplV9.class);
        when(sentryConnectorHolderMock.getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 400)));
        sentrySyncProcessor = new SentrySyncProcessor(
                new Properties(),
                sentryConnectorHolderMock,
                metricsCollectorMock,
                router,
                "",
                "");
        sentrySyncProcessor.process(EVENT);

        verify(sentryConnectorHolderMock, times(1)).getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT);
    }

    @Test(expected = BackendServiceFailedException.class)
    public void shouldThrowExceptionWhenHappensOtherErrorOfApiClient() throws BackendServiceFailedException {
        when(sentryConnectorHolderMock.getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 403)));
        when(sentryConnectorMock.getSentryClient()).thenReturn(herculesSentryClientMock);
        doNothing().when(herculesSentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        sentrySyncProcessor.process(EVENT);
    }

    @Test
    public void shouldNotRetryWhenHappensOtherErrorOfApiClient() {
        sentryConnectorHolderMock = mock(SentryConnectorHolderImplV9.class);
        sentrySyncProcessor = new SentrySyncProcessor(
                new Properties(),
                sentryConnectorHolderMock,
                metricsCollectorMock,
                router,
                "",
                "");
        when(sentryConnectorHolderMock.getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 403)));
        doNothing().when(herculesSentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        try {
            sentrySyncProcessor.process(EVENT);
        } catch (BackendServiceFailedException e) {

            verify(sentryConnectorHolderMock, times(1)).getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT);
        }
    }

    @Test
    public void shouldReturnFalseWhenHappensTooManyRequestsExceptionOfSending() throws BackendServiceFailedException {
        sentryConnectorMock = mock(SentryConnectorImplV9.class);
        sentryConnectorHolderMock = mock(SentryConnectorHolderImplV9.class);
        herculesSentryClientMock = mock(HerculesSentryClient.class);
        sentrySyncProcessor = new SentrySyncProcessor(
                new Properties(),
                sentryConnectorHolderMock,
                metricsCollectorMock,
                router,
                "",
                "");
        when(sentryConnectorHolderMock.getOrCreateConnector(anyString(), anyString()))
                .thenReturn(Result.ok(sentryConnectorMock));
        when(sentryConnectorMock.getSentryClient()).thenReturn(herculesSentryClientMock);
        doThrow(new TooManyRequestsException("TooManyRequestsException", new Exception(), 60000L, 429))
                .when(herculesSentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        boolean result = sentrySyncProcessor.process(EVENT);

        assertFalse(result);
    }

    @Test
    public void shouldReturnFalseWhenHappensInvalidDsnExceptionOfSending() throws BackendServiceFailedException {
        when(sentryConnectorHolderMock.getOrCreateConnector(anyString(), anyString())).thenReturn(
                Result.ok(sentryConnectorMock));
        when(sentryConnectorMock.getSentryClient()).thenReturn(herculesSentryClientMock);
        doThrow(InvalidDsnException.class).when(herculesSentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        boolean result = sentrySyncProcessor.process(EVENT);

        assertFalse(result);
    }

    @Test
    public void shouldNotRetryWhenHappensInvalidDsnExceptionOfSending() throws BackendServiceFailedException {
        sentryConnectorMock = mock(SentryConnectorImplV9.class);
        sentryConnectorHolderMock = mock(SentryConnectorHolderImplV9.class);
        sentrySyncProcessor = new SentrySyncProcessor(
                new Properties(),
                sentryConnectorHolderMock,
                metricsCollectorMock,
                router,
                "",
                "");
        when(sentryConnectorHolderMock.getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryConnectorMock));
        when(sentryConnectorMock.getSentryClient()).thenReturn(herculesSentryClientMock);
        doThrow(InvalidDsnException.class).when(herculesSentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        sentrySyncProcessor.process(EVENT);

        verify(sentryConnectorHolderMock, times(1)).getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT);
    }

    @Test(expected = BackendServiceFailedException.class)
    public void shouldThrowExceptionWhenHappensExceptionWithRetryableCode() throws BackendServiceFailedException {
        sentryConnectorMock = mock(SentryConnectorImplV9.class);
        sentryConnectorHolderMock = mock(SentryConnectorHolderImplV9.class);
        herculesSentryClientMock = mock(HerculesSentryClient.class);
        sentrySyncProcessor = new SentrySyncProcessor(
                new Properties(),
                sentryConnectorHolderMock,
                metricsCollectorMock,
                router,
                "",
                "");
        when(sentryConnectorHolderMock.getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryConnectorMock));
        when(sentryConnectorMock.getSentryClient()).thenReturn(herculesSentryClientMock);
        doThrow(new ConnectionException("ConnectionException", new Exception(), null, 401))
                .when(herculesSentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        sentrySyncProcessor.process(EVENT);
    }


    @Test
    public void shouldRetryWhenHappensExceptionWithRetryableCode() {
        sentryConnectorMock = mock(SentryConnectorImplV9.class);
        sentryConnectorHolderMock = mock(SentryConnectorHolderImplV9.class);
        herculesSentryClientMock = mock(HerculesSentryClient.class);
        sentrySyncProcessor = new SentrySyncProcessor(
                new Properties(),
                sentryConnectorHolderMock,
                metricsCollectorMock,
                router,
                "",
                "");
        when(sentryConnectorHolderMock.getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryConnectorMock));
        when(sentryConnectorMock.getSentryClient()).thenReturn(herculesSentryClientMock);
        doThrow(new ConnectionException("ConnectionException", new Exception(), null, 401))
                .when(herculesSentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        try {
            sentrySyncProcessor.process(EVENT);
        } catch (BackendServiceFailedException e) {
            verify(herculesSentryClientMock, times(RETRY_COUNT + 1))
                    .sendEvent(any(io.sentry.event.Event.class));
        }
    }

    @Test
    public void shouldReturnFalseWhenHappensConnectionException() throws BackendServiceFailedException {
        sentryConnectorMock = mock(SentryConnectorImplV9.class);
        sentryConnectorHolderMock = mock(SentryConnectorHolderImplV9.class);
        sentrySyncProcessor = new SentrySyncProcessor(
                new Properties(),
                sentryConnectorHolderMock,
                metricsCollectorMock,
                router,
                "",
                "");
        when(sentryConnectorHolderMock.getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryConnectorMock));
        when(sentryConnectorMock.getSentryClient()).thenReturn(herculesSentryClientMock);
        doThrow(new ConnectionException("ConnectionException", new Exception(), null, 400))
                .when(herculesSentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        boolean result = sentrySyncProcessor.process(EVENT);

        assertFalse(result);
    }

    @Test
    public void shouldNotRetryWhenHappensExceptionWithNonRetryableCode() throws BackendServiceFailedException {
        sentryConnectorMock = mock(SentryConnectorImplV9.class);
        sentryConnectorHolderMock = mock(SentryConnectorHolderImplV9.class);
        herculesSentryClientMock = mock(HerculesSentryClient.class);
        sentrySyncProcessor = new SentrySyncProcessor(
                new Properties(),
                sentryConnectorHolderMock,
                metricsCollectorMock,
                router,
                "",
                "");
        when(sentryConnectorHolderMock.getOrCreateConnector(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryConnectorMock));
        when(sentryConnectorMock.getSentryClient()).thenReturn(herculesSentryClientMock);
        doThrow(new ConnectionException("ConnectionException", new Exception(), null, 400))
                .when(herculesSentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        sentrySyncProcessor.process(EVENT);

        verify(herculesSentryClientMock, times(1)).sendEvent(any(io.sentry.event.Event.class));
    }

    private static Event createEvent() {
        return EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag(CommonTags.PROJECT_TAG.getName(), Variant.ofString(MY_ORGANIZATION))
                        .tag(CommonTags.SUBPROJECT_TAG.getName(), Variant.ofString(MY_PROJECT))
                        .tag(CommonTags.ENVIRONMENT_TAG.getName(), Variant.ofString(MY_ENVIRONMENT))
                        .build()
                ))
                .tag(LogEventTags.LEVEL_TAG.getName(), Variant.ofString("Error"))
                .build();
    }
}