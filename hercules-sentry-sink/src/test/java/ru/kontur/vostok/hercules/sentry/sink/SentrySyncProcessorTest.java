package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import io.sentry.connection.ConnectionException;
import io.sentry.dsn.InvalidDsnException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryEventConverter;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
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

    private SentryClientHolder sentryClientHolderMock = mock(SentryClientHolder.class);
    private SentryClient sentryClientMock = mock(SentryClient.class);

    private static MetricsCollector metricsCollectorMock = mock(MetricsCollector.class);

    private SentrySyncProcessor sentrySyncProcessor = new SentrySyncProcessor(
            new Properties(),
            sentryClientHolderMock,
            new SentryEventConverter("0.0.0"),
            metricsCollectorMock);
    private static UUID someUuid = UUID.randomUUID();
    private static final String MY_PROJECT = "my-project";
    private static final String MY_ORGANIZATION = "my-organization";
    private static final String MY_APPLICATION = "my-application";
    private static final String MY_ENVIRONMENT = "test";
    private static final int RETRY_COUNT = 3;
    private static final Event EVENT = createEvent();
    private static final String CLIENT_API_ERROR = "ClientApiError";

    /**
     * Mock metrics
     */
    @BeforeClass
    public static void init() {
        when(metricsCollectorMock.meter(anyString())).thenReturn(n -> {
        });
        when(metricsCollectorMock.timer(anyString())).thenReturn((duration, unit) -> {
        });
    }

    @Test
    public void shouldReturnFalseWhenProcessEventWithoutLevelTag() throws BackendServiceFailedException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.PROJECT_TAG, Variant.ofString(MY_ORGANIZATION))
                        .tag(CommonTags.APPLICATION_TAG, Variant.ofString(MY_APPLICATION))
                        .tag(CommonTags.ENVIRONMENT_TAG, Variant.ofString(MY_ENVIRONMENT))
                        .build()
                ))
                .build();

        assertFalse(sentrySyncProcessor.process(event));
    }

    @Test
    public void shouldReturnFalseWhenProcessEventWithLowValueOfLevelTag() throws BackendServiceFailedException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.PROJECT_TAG, Variant.ofString(MY_ORGANIZATION))
                        .tag(CommonTags.APPLICATION_TAG, Variant.ofString(MY_APPLICATION))
                        .tag(CommonTags.ENVIRONMENT_TAG, Variant.ofString(MY_ENVIRONMENT))
                        .build()
                ))
                .tag(LogEventTags.LEVEL_TAG, Variant.ofString("debug"))
                .build();
        assertFalse(sentrySyncProcessor.process(event));
    }

    @Test
    public void shouldReturnFalseWhenProcessEventWithoutPropertiesTag() throws BackendServiceFailedException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(LogEventTags.LEVEL_TAG, Variant.ofString("Error"))
                .build();

        assertFalse(sentrySyncProcessor.process(event));
    }

    @Test
    public void shouldReturnFalseWhenProcessEventWithoutProjectTag() throws BackendServiceFailedException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.APPLICATION_TAG, Variant.ofString(MY_APPLICATION))
                        .tag(CommonTags.ENVIRONMENT_TAG, Variant.ofString(MY_ENVIRONMENT))
                        .build()
                ))
                .tag(LogEventTags.LEVEL_TAG, Variant.ofString("Error"))
                .build();

        assertFalse(sentrySyncProcessor.process(event));
    }

    @Test
    public void shouldReturnTrueWhenProcessEventWithoutApplicationTag() throws BackendServiceFailedException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.PROJECT_TAG, Variant.ofString(MY_ORGANIZATION))
                        .tag(CommonTags.ENVIRONMENT_TAG, Variant.ofString(MY_ENVIRONMENT))
                        .build()
                ))
                .tag(LogEventTags.LEVEL_TAG, Variant.ofString("Error"))
                .build();
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_ORGANIZATION))
                .thenReturn(Result.ok(sentryClientMock));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        assertTrue(sentrySyncProcessor.process(event));
    }

    @Test
    public void shouldSetSentryProjectBySubprojectTag() throws BackendServiceFailedException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.PROJECT_TAG, Variant.ofString(MY_ORGANIZATION))
                        .tag(CommonTags.SUBPROJECT_TAG, Variant.ofString(MY_PROJECT))
                        .tag(CommonTags.APPLICATION_TAG, Variant.ofString(MY_APPLICATION))
                        .build()
                ))
                .tag(LogEventTags.LEVEL_TAG, Variant.ofString("Error"))
                .build();
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryClientMock));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        assertTrue(sentrySyncProcessor.process(event));
    }

    @Test
    public void shouldSetSentryProjectByApplicationTag() throws BackendServiceFailedException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.PROJECT_TAG, Variant.ofString(MY_ORGANIZATION))
                        .tag(CommonTags.APPLICATION_TAG, Variant.ofString(MY_APPLICATION))
                        .build()
                ))
                .tag(LogEventTags.LEVEL_TAG, Variant.ofString("Error"))
                .build();
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_APPLICATION))
                .thenReturn(Result.ok(sentryClientMock));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        assertTrue(sentrySyncProcessor.process(event));
    }

    @Test(expected = BackendServiceFailedException.class)
    public void shouldThrowExceptionWhenHappensRetryableErrorOfApiClient() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 404)));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        sentrySyncProcessor.process(EVENT);
    }

    @Test
    public void shouldRetryWhenHappensRetryableErrorOfApiClient() {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 404)));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        try {
            sentrySyncProcessor.process(EVENT);
        } catch (BackendServiceFailedException e) {

            verify(sentryClientHolderMock, times(RETRY_COUNT + 1)).getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);
        }
    }

    @Test
    public void shouldReturnFalseWhenHappensNonRetryableErrorOfApiClient() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 400)));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        boolean result = sentrySyncProcessor.process(EVENT);

        assertFalse(result);
    }

    @Test
    public void shouldNotRetryWhenHappensNonRetryableErrorOfApiClient() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 400)));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        sentrySyncProcessor.process(EVENT);

        verify(sentryClientHolderMock, times(1)).getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);
    }

    @Test(expected = BackendServiceFailedException.class)
    public void shouldThrowExceptionWhenHappensOtherErrorOfApiClient() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 403)));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        sentrySyncProcessor.process(EVENT);
    }

    @Test
    public void shouldNotRetryWhenHappensOtherErrorOfApiClient() {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(CLIENT_API_ERROR, 403)));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        try {
            sentrySyncProcessor.process(EVENT);
        } catch (BackendServiceFailedException e) {

            verify(sentryClientHolderMock, times(1)).getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);
        }
    }

    @Test
    public void shouldReturnFalseWhenHappensInvalidDsnExceptionOfSending() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryClientMock));
        doThrow(InvalidDsnException.class).when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        boolean result = sentrySyncProcessor.process(EVENT);

        assertFalse(result);
    }

    @Test
    public void shouldNotRetryWhenHappensInvalidDsnExceptionOfSending() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryClientMock));
        doThrow(InvalidDsnException.class).when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        sentrySyncProcessor.process(EVENT);

        verify(sentryClientHolderMock, times(1)).getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);
    }

    @Test(expected = BackendServiceFailedException.class)
    public void shouldThrowExceptionWhenHappensConnectionExceptionWithRetryableCode() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryClientMock));
        doThrow(new ConnectionException("ConnectionException", new Exception(), null, 401))
                .when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        sentrySyncProcessor.process(EVENT);
    }


    @Test
    public void shouldRetryWhenHappensConnectionExceptionWithRetryableCode() {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryClientMock));
        doThrow(new ConnectionException("ConnectionException", new Exception(), null, 401))
                .when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        try {
            sentrySyncProcessor.process(EVENT);
        } catch (BackendServiceFailedException e) {

            verify(sentryClientMock, times(RETRY_COUNT + 1)).sendEvent(any(io.sentry.event.Event.class));
        }
    }

    @Test
    public void shouldReturnFalseWhenHappensConnectionExceptionWithNonRetryableCode() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryClientMock));
        doThrow(new ConnectionException("ConnectionException", new Exception(), null, 400))
                .when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        boolean result = sentrySyncProcessor.process(EVENT);

        assertFalse(result);
    }

    @Test
    public void shouldNotRetryWhenHappensConnectionExceptionWithNonRetryableCode() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryClientMock));
        doThrow(new ConnectionException("ConnectionException", new Exception(), null, 400))
                .when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        sentrySyncProcessor.process(EVENT);

        verify(sentryClientMock, times(1)).sendEvent(any(io.sentry.event.Event.class));
    }

    private static Event createEvent() {
        return EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.PROJECT_TAG, Variant.ofString(MY_ORGANIZATION))
                        .tag(CommonTags.APPLICATION_TAG, Variant.ofString(MY_PROJECT))
                        .tag(CommonTags.ENVIRONMENT_TAG, Variant.ofString(MY_ENVIRONMENT))
                        .build()
                ))
                .tag(LogEventTags.LEVEL_TAG, Variant.ofString("Error"))
                .build();
    }
}