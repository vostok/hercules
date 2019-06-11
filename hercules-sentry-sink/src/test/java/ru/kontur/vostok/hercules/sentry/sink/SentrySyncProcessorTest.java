package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
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

    @Test
    public void shouldHandleRetryableErrorOfApiClient() throws BackendServiceFailedException {
        final Event event = createEvent();
        when(sentryClientHolderMock.getOrCreateClient(MY_ORGANIZATION, MY_PROJECT))
                .thenReturn(Result.error(new ErrorInfo(404)));
        doNothing().when(sentryClientMock).sendEvent(any(io.sentry.event.Event.class));
        boolean result = sentrySyncProcessor.process(someUuid, event);

        verify(sentryClientHolderMock, times(4)).getOrCreateClient(MY_ORGANIZATION, MY_PROJECT);
        assertFalse(result);
    }

    public Event createEvent() {
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