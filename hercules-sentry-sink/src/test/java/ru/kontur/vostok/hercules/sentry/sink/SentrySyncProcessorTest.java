package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import io.sentry.connection.ConnectionException;
import io.sentry.dsn.InvalidDsnException;
import org.junit.BeforeClass;
import org.junit.Test;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.functional.Result;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SentrySyncProcessorTest {

    private SentryClientHolder sentryClientHolderMock = mock(SentryClientHolder.class);
    private SentrySyncProcessor sentrySyncProcessor = new SentrySyncProcessor(new Properties(), sentryClientHolderMock);
    private SentryClient sentryClientMock = mock(SentryClient.class);

    private static UUID someUuid = UUID.randomUUID();
    private static final String MY_PROJECT = "my-project";
    private static final String MY_ORGANIZATION = "my-organization";
    private static final String MY_ENVIRONMENT = "test";
    private static final int RETRY_COUNT = 3;
    private static final Event EVENT = createEvent();

    /**
     * Init application context.
     * It is necessary to execute "SentryEventConverter.convert(event)"
     * in the method "process(UUID key, Event event)" of {@link SentrySyncProcessor}
     */
    @BeforeClass
    public static void init() {
        Properties properties = new Properties();
        properties.setProperty("instance.id", "1");
        properties.setProperty("environment", MY_ENVIRONMENT);
        properties.setProperty("zone", "1");
        ApplicationContextHolder.init("appName", "appId", properties);
    }

    @Test
    public void shouldProcessEventWithoutLevelTag() throws BackendServiceFailedException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.PROJECT_TAG, Variant.ofString(MY_ORGANIZATION))
                        .tag(CommonTags.APPLICATION_TAG, Variant.ofString(MY_PROJECT))
                        .tag(CommonTags.ENVIRONMENT_TAG, Variant.ofString(MY_ENVIRONMENT))
                        .build()
                ))
                .build();

        assertFalse(sentrySyncProcessor.process(someUuid, event));
    }

    @Test
    public void shouldProcessEventWithoutPropertiesTag() throws BackendServiceFailedException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(LogEventTags.LEVEL_TAG, Variant.ofString("Error"))
                .build();

        assertFalse(sentrySyncProcessor.process(someUuid, event));
    }

    @Test
    public void shouldProcessEventWithoutProjectTag() throws BackendServiceFailedException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.APPLICATION_TAG, Variant.ofString(MY_PROJECT))
                        .tag(CommonTags.ENVIRONMENT_TAG, Variant.ofString(MY_ENVIRONMENT))
                        .build()
                ))
                .tag(LogEventTags.LEVEL_TAG, Variant.ofString("Error"))
                .build();

        assertFalse(sentrySyncProcessor.process(someUuid, event));
    }

    @Test
    public void shouldProcessEventWithoutApplicationTag() throws BackendServiceFailedException {
        final Event event = EventBuilder.create(TimeUtil.UNIX_EPOCH, someUuid.toString())
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(CommonTags.PROJECT_TAG, Variant.ofString(MY_PROJECT))
                        .tag(CommonTags.ENVIRONMENT_TAG, Variant.ofString(MY_ENVIRONMENT))
                        .build()
                ))
                .tag(LogEventTags.LEVEL_TAG, Variant.ofString("Error"))
                .build();
        when(sentryClientHolderMock.getOrCreateClient(MY_PROJECT, MY_PROJECT))
                .thenReturn(Result.ok(sentryClientMock));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        assertTrue(sentrySyncProcessor.process(someUuid, event));
    }

    @Test(expected = BackendServiceFailedException.class)
    public void shouldHandleRetryableErrorOfApiClient() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(404)));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        try {
            sentrySyncProcessor.process(someUuid, EVENT);
        } catch (BackendServiceFailedException e) {

            verify(sentryClientHolderMock, times(RETRY_COUNT + 1)).getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);
            throw e;
        }
    }

    @Test
    public void shouldHandleNonRetryableErrorOfApiClient() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(400)));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        boolean result = sentrySyncProcessor.process(someUuid, EVENT);

        verify(sentryClientHolderMock).getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);
        assertFalse(result);
    }

    @Test(expected = BackendServiceFailedException.class)
    public void shouldHandleErrorWithExceptionOfApiClient() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(402)));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        try {
            sentrySyncProcessor.process(someUuid, EVENT);
        } catch (BackendServiceFailedException e) {

            verify(sentryClientHolderMock).getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);
            throw e;
        }
    }

    @Test
    public void shouldHandleInvalidDsnExceptionOfSending() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryClientMock));
        doThrow(InvalidDsnException.class).when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        boolean result = sentrySyncProcessor.process(someUuid, EVENT);

        verify(sentryClientHolderMock).getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);
        assertFalse(result);
    }

    @Test(expected = BackendServiceFailedException.class)
    public void shouldHandleConnectionExceptionWithRetryableCode() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryClientMock));

        doThrow(new ConnectionException("UNAUTHORIZED", new Exception(), null, 401))
                .when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));

        try {
            sentrySyncProcessor.process(someUuid, EVENT);
        } catch (BackendServiceFailedException e) {

            verify(sentryClientMock, times(RETRY_COUNT + 1)).sendEvent(any(io.sentry.event.Event.class));
            throw e;
        }
    }

    @Test
    public void shouldHandleConnectionExceptionWithNonRetryableCode() throws BackendServiceFailedException {
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.ok(sentryClientMock));
        doThrow(new ConnectionException("BED_REQUEST", new Exception(), null, 400))
                .when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        boolean result = sentrySyncProcessor.process(someUuid, EVENT);

        verify(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        assertFalse(result);
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