package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.Event;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import io.sentry.event.interfaces.SentryStackTraceElement;
import io.sentry.event.interfaces.UserInterface;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.util.ContainerBuilder;
import ru.kontur.vostok.hercules.protocol.util.EventBuilder;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.tags.SentryTags;
import ru.kontur.vostok.hercules.tags.StackFrameTags;
import ru.kontur.vostok.hercules.tags.UserTags;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Petr Demenev
 */
public class SentryEventConverterTest {

    private static final String someUuid = "00000000-0000-1000-994f-8fcf383f0000";
    private static final String platformFromStacktrace = "java";

    private static Container createException() {
        return ContainerBuilder.create()
                .tag(ExceptionTags.TYPE_TAG, Variant.ofString("com.example.test.exceptions.ExceptionClass"))
                .tag(ExceptionTags.MESSAGE_TAG, Variant.ofString("Some error of ExceptionClass happened"))
                .tag(ExceptionTags.STACK_FRAMES, Variant.ofVector(Vector.ofContainers(
                        ContainerBuilder.create()
                                .tag(StackFrameTags.TYPE_TAG, Variant.ofString("com.example.test.SomeModule"))
                                .tag(StackFrameTags.FUNCTION_TAG, Variant.ofString("function"))
                                .tag(StackFrameTags.FILE_TAG, Variant.ofString("SomeModule.java"))
                                .tag(StackFrameTags.LINE_NUMBER_TAG, Variant.ofInteger(100))
                                .tag(StackFrameTags.COLUMN_NUMBER_TAG, Variant.ofShort((short) 12))
                                .build(),
                        ContainerBuilder.create()
                                .tag(StackFrameTags.TYPE_TAG, Variant.ofString("com.example.test.AnotherModule"))
                                .tag(StackFrameTags.FUNCTION_TAG, Variant.ofString("function"))
                                .tag(StackFrameTags.FILE_TAG, Variant.ofString("AnotherModule.java"))
                                .tag(StackFrameTags.LINE_NUMBER_TAG, Variant.ofInteger(200))
                                .tag(StackFrameTags.COLUMN_NUMBER_TAG, Variant.ofShort((short) 13))
                                .build()
                )))
                .build();
    }

    @Before
    public void setUp() {
        Properties testProperties = new Properties();
        testProperties.setProperty("environment", "test");
        testProperties.setProperty("instance.id", "test");
        testProperties.setProperty("zone", "test");
        ApplicationContextHolder.init("test", "test", testProperties);
    }

    @Test
    public void shouldConvertEventWithMessage() {
        final String message = "This is message sample";
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.MESSAGE_TAG, Variant.ofString(message))
                .build();

        final Event sentryEvent = SentryEventConverter.convert(event);

        Assert.assertEquals(message, sentryEvent.getMessage());
    }

    @Test
    public void shouldConvertEventWithExceptions() {
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.EXCEPTION_TAG, Variant.ofContainer(createException()))
                .build();

        Event sentryEvent = SentryEventConverter.convert(event);

        ExceptionInterface exceptionInterface = (ExceptionInterface) sentryEvent.getSentryInterfaces().get("sentry.interfaces.Exception");
        SentryException exception = exceptionInterface.getExceptions().getFirst();

        Assert.assertEquals("Some error of ExceptionClass happened", exception.getExceptionMessage());
        Assert.assertEquals("ExceptionClass", exception.getExceptionClassName());
        Assert.assertEquals("com.example.test.exceptions", exception.getExceptionPackageName());

        SentryStackTraceElement[] stackTrace = exception.getStackTraceInterface().getStackTrace();
        Assert.assertEquals(2, stackTrace.length);

        Assert.assertEquals("com.example.test.SomeModule", stackTrace[0].getModule());
        Assert.assertEquals("function", stackTrace[0].getFunction());
        Assert.assertEquals("SomeModule.java", stackTrace[0].getFileName());
        Assert.assertEquals(100, stackTrace[0].getLineno());
        Assert.assertEquals(12, (int) stackTrace[0].getColno());
        Assert.assertEquals("SomeModule.java", stackTrace[0].getAbsPath());

        Assert.assertEquals("com.example.test.AnotherModule", stackTrace[1].getModule());
        Assert.assertEquals("function", stackTrace[1].getFunction());
        Assert.assertEquals("AnotherModule.java", stackTrace[1].getFileName());
        Assert.assertEquals(200, stackTrace[1].getLineno());
        Assert.assertEquals(13, (int) stackTrace[1].getColno());
        Assert.assertEquals("AnotherModule.java", stackTrace[1].getAbsPath());
    }

    @Test
    public void shouldExtractPlatformValue() {
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.EXCEPTION_TAG, Variant.ofContainer(createException()))
                .build();

        final Event sentryEvent = SentryEventConverter.convert(event);

        Assert.assertEquals(platformFromStacktrace, sentryEvent.getPlatform());
    }

    @Test
    public void shouldSetPlatformIfPlatformTagAbsentInPropertiesTag() {
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.EXCEPTION_TAG, Variant.ofContainer(createException()))
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .build()
                ))
                .build();

        final Event sentryEvent = SentryEventConverter.convert(event);

        Assert.assertEquals(platformFromStacktrace, sentryEvent.getPlatform());
    }

    @Test
    public void shouldSetAttributes() {
        final String transaction = "my_transaction";
        final String release = "my_release 0.1.0";
        final String fingerprintWord1 = "{{ default }}";
        final String fingerprintWord2 = "my_label";
        final String platform = "Python";
        final String logger = "Log4j";
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(SentryTags.TRACE_ID_TAG, Variant.ofString(transaction))
                        .tag(SentryTags.RELEASE_TAG, Variant.ofString(release))
                        .tag(SentryTags.FINGERPRINT_TAG, Variant.ofVector(Vector.ofStrings(fingerprintWord1, fingerprintWord2)))
                        .tag(SentryTags.PLATFORM_TAG, Variant.ofString(platform))
                        .tag(SentryTags.LOGGER_TAG, Variant.ofString(logger))
                        .build()
                ))
                .build();

        final Event sentryEvent = SentryEventConverter.convert(event);

        Assert.assertEquals(transaction, sentryEvent.getTransaction());
        Assert.assertEquals(release, sentryEvent.getRelease());
        Assert.assertTrue(sentryEvent.getFingerprint().contains(fingerprintWord1));
        Assert.assertTrue(sentryEvent.getFingerprint().contains(fingerprintWord2));
        Assert.assertEquals(2, sentryEvent.getFingerprint().size());
        Assert.assertEquals(platform.toLowerCase(), sentryEvent.getPlatform());
        Assert.assertEquals(logger, sentryEvent.getLogger());
    }

    @Test
    public void shouldSetUser() {
        final String id = "my_id";
        final String username = "my_username";
        final String ipAddress = "11.22.33.44";
        final String email = "test@company.com";

        final String uuid_key = "uuid_key";
        final UUID uuid_value = UUID.randomUUID();
        final String null_key = "null_key";

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(SentryTags.USER_TAG, Variant.ofContainer(ContainerBuilder.create()
                                .tag(UserTags.ID_TAG, Variant.ofString(id))
                                .tag(UserTags.USERNAME_TAG, Variant.ofString(username))
                                .tag(UserTags.IP_ADDRESS_TAG, Variant.ofString(ipAddress))
                                .tag(UserTags.EMAIL_TAG, Variant.ofString(email))
                                .tag(uuid_key, Variant.ofUuid(uuid_value))
                                .tag(null_key, Variant.ofNull())
                                .build()
                        ))
                        .build()
                ))
                .build();

        final Event sentryEvent = SentryEventConverter.convert(event);

        UserInterface userInterface = (UserInterface) sentryEvent.getSentryInterfaces().get("sentry.interfaces.User");
        Assert.assertEquals(id, userInterface.getId());
        Assert.assertEquals(username, userInterface.getUsername());
        Assert.assertEquals(ipAddress, userInterface.getIpAddress());
        Assert.assertEquals(email, userInterface.getEmail());
        Assert.assertEquals(uuid_value, userInterface.getData().get(uuid_key));
        Assert.assertNull(userInterface.getData().get(null_key));
    }

    @Test
    public void shouldSetContext() {
        final String name = "My Browser";
        final String version = "79.3.150";
        final String raw_description = ".NET Framework 4.7.3163.0";
        final boolean rooted = true;

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(SentryTags.CONTEXTS_TAG, Variant.ofContainer(ContainerBuilder.create()
                                .tag("browser", Variant.ofContainer(ContainerBuilder.create()
                                        .tag("name", Variant.ofString(name))
                                        .tag("version", Variant.ofString(version))
                                        .build()
                                ))
                                .tag("os", Variant.ofContainer(ContainerBuilder.create()
                                        .tag("raw_description", Variant.ofString(raw_description))
                                        .tag("rooted", Variant.ofFlag(rooted))
                                        .build()
                                ))
                                .build()
                        ))
                        .build()
                ))
                .build();

        final Event sentryEvent = SentryEventConverter.convert(event);

        Map<String, Map<String, Object>> contexts = sentryEvent.getContexts();
        Assert.assertEquals(name, contexts.get("browser").get("name"));
        Assert.assertEquals(version, contexts.get("browser").get("version"));
        Assert.assertEquals(raw_description, contexts.get("os").get("raw_description"));
        Assert.assertTrue((boolean) contexts.get("os").get("rooted"));
    }

    @Test
    public void shouldSetAdditionalTags() {
        final String stringKey = "my_string_tag";
        final String stringValue = "My string";
        final String longKey = "my_long_tag";
        final long longValue = 100500100500L;
        final String booleanKey = "my_boolean_tag";
        final boolean booleanValue = true;

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(stringKey, Variant.ofString(stringValue))
                        .tag(longKey, Variant.ofLong(longValue))
                        .tag(booleanKey, Variant.ofFlag(booleanValue))
                        .build()
                ))
                .build();

        final Event sentryEvent = SentryEventConverter.convert(event);

        Assert.assertEquals(stringValue, sentryEvent.getTags().get(stringKey));
        Assert.assertEquals(String.valueOf(longValue), sentryEvent.getTags().get(longKey));
        Assert.assertEquals(String.valueOf(booleanValue), sentryEvent.getTags().get(booleanKey));
    }

    @Test
    public void shouldSetExtra() {
        final String stringKey = "my_string_tag";
        final String stringValue = "My string";
        final String containerKey = "my_container_tag";
        final String intKey = "my_int_tag";
        final int intValue = 100500;

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(SentryTags.EXTRA_TAG, Variant.ofContainer(ContainerBuilder.create()
                                .tag(stringKey, Variant.ofString(stringValue))
                                .tag(containerKey, Variant.ofContainer(ContainerBuilder.create()
                                        .tag(intKey, Variant.ofInteger(intValue))
                                        .build()
                                ))
                                .build()
                        ))
                        .build()
                ))
                .build();

        final Event sentryEvent = SentryEventConverter.convert(event);

        Assert.assertEquals(stringValue, sentryEvent.getExtra().get(stringKey));
        Assert.assertEquals(intValue, ((Map) (sentryEvent.getExtra().get(containerKey))).get(intKey));
    }

    @Test
    public void shouldNotSetUnknownPlatform() {
        final String unknownPlatform = "pascal";
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.EXCEPTION_TAG, Variant.ofContainer(createException()))
                .tag(CommonTags.PROPERTIES_TAG, Variant.ofContainer(ContainerBuilder.create()
                        .tag(SentryTags.PLATFORM_TAG, Variant.ofString(unknownPlatform))
                        .build()
                ))
                .build();

        final Event sentryEvent = SentryEventConverter.convert(event);

        Assert.assertEquals(platformFromStacktrace, sentryEvent.getPlatform());
    }
}
