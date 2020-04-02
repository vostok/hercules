package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.Event;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import io.sentry.event.interfaces.SentryStackTraceElement;
import io.sentry.event.interfaces.UserInterface;
import org.junit.Assert;
import org.junit.Test;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.tags.SentryTags;
import ru.kontur.vostok.hercules.tags.StackFrameTags;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Petr Demenev
 */
public class SentryEventConverterTest {

    private static final String someUuid = "00000000-0000-1000-994f-8fcf383f0000";
    private static final String platformFromStacktrace = "java";

    private static final SentryEventConverter SENTRY_EVENT_CONVERTER = new SentryEventConverter("0.0.0");

    private static Container createException() {
        return Container.builder()
                .tag(ExceptionTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.exceptions.ExceptionClass"))
                .tag(ExceptionTags.MESSAGE_TAG.getName(), Variant.ofString("Some error of ExceptionClass happened"))
                .tag(ExceptionTags.STACK_FRAMES.getName(), Variant.ofVector(Vector.ofContainers(
                        Container.builder()
                                .tag(StackFrameTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.SomeModule"))
                                .tag(StackFrameTags.FUNCTION_TAG.getName(), Variant.ofString("function"))
                                .tag(StackFrameTags.FILE_TAG.getName(), Variant.ofString("SomeModule.java"))
                                .tag(StackFrameTags.LINE_NUMBER_TAG.getName(), Variant.ofInteger(100))
                                .tag(StackFrameTags.COLUMN_NUMBER_TAG.getName(), Variant.ofShort((short) 12))
                                .build(),
                        Container.builder()
                                .tag(StackFrameTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.AnotherModule"))
                                .tag(StackFrameTags.FUNCTION_TAG.getName(), Variant.ofString("function"))
                                .tag(StackFrameTags.FILE_TAG.getName(), Variant.ofString("AnotherModule.java"))
                                .tag(StackFrameTags.LINE_NUMBER_TAG.getName(), Variant.ofInteger(200))
                                .tag(StackFrameTags.COLUMN_NUMBER_TAG.getName(), Variant.ofShort((short) 13))
                                .build()
                )))
                .build();
    }

    @Test
    public void shouldConvertEventWithMessage() {
        final String message = "This is message sample";
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.MESSAGE_TAG.getName(), Variant.ofString(message))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertEquals(message, sentryEvent.getMessage());
    }

    @Test
    public void shouldConvertEventWithExceptions() {
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .build();

        Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

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
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertEquals(platformFromStacktrace, sentryEvent.getPlatform());
    }

    @Test
    public void shouldSetPlatformIfPlatformTagAbsentInPropertiesTag() {
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.empty()))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertEquals(platformFromStacktrace, sentryEvent.getPlatform());
    }

    @Test
    public void shouldSetAttributes() {
        final String traceId = "my_trace_ID";
        final String release = "my_release 0.1.0";
        final String fingerprintWord1 = "{{ default }}";
        final String fingerprintWord2 = "my_label";
        final String platform = "Python";
        final String logger = "Log4j";
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag(SentryTags.TRACE_ID_TAG.getName(), Variant.ofString(traceId))
                        .tag(SentryTags.RELEASE_TAG.getName(), Variant.ofString(release))
                        .tag(SentryTags.FINGERPRINT_TAG.getName(), Variant.ofVector(Vector.ofStrings(fingerprintWord1, fingerprintWord2)))
                        .tag(SentryTags.PLATFORM_TAG.getName(), Variant.ofString(platform))
                        .tag(SentryTags.LOGGER_TAG.getName(), Variant.ofString(logger))
                        .build()
                ))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertEquals(traceId, sentryEvent.getTransaction());
        Assert.assertEquals(traceId, sentryEvent.getTags().get("traceId"));
        Assert.assertEquals(release, sentryEvent.getRelease());
        Assert.assertTrue(sentryEvent.getFingerprint().contains(fingerprintWord1));
        Assert.assertTrue(sentryEvent.getFingerprint().contains(fingerprintWord2));
        Assert.assertEquals(2, sentryEvent.getFingerprint().size());
        Assert.assertEquals(platform.toLowerCase(), sentryEvent.getPlatform());
        Assert.assertEquals(logger, sentryEvent.getLogger());
    }

    @Test
    public void shouldSetFingerprintByMessageTemplate() {
        final String messageTemplate = "My message template";
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.MESSAGE_TEMPLATE_TAG.getName(), Variant.ofString(messageTemplate))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertTrue(sentryEvent.getFingerprint().contains(messageTemplate));
        Assert.assertEquals(1, sentryEvent.getFingerprint().size());
    }

    @Test
    public void shouldNotSetFingerprintByMessageTemplateIfExceptionExists() {
        final String messageTemplate = "My message template";
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.MESSAGE_TEMPLATE_TAG.getName(), Variant.ofString(messageTemplate))
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertNull(sentryEvent.getFingerprint());
    }

    @Test
    public void shouldNotSetFingerprintByMessageTemplateIfItIsSetExplicitly() {
        final String messageTemplate = "My message template";
        final String fingerprint = "my_label";
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.MESSAGE_TEMPLATE_TAG.getName(), Variant.ofString(messageTemplate))
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.FINGERPRINT_TAG.getName(), Variant.ofVector(Vector.ofStrings(fingerprint)))))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertTrue(sentryEvent.getFingerprint().contains(fingerprint));
        Assert.assertEquals(1, sentryEvent.getFingerprint().size());
    }

    @Test
    public void shouldNotSetUnknownPlatform() {
        final String unknownPlatform = "pascal";
        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.PLATFORM_TAG.getName(), Variant.ofString(unknownPlatform))))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertEquals(platformFromStacktrace, sentryEvent.getPlatform());
    }

    @Test
    public void shouldSetUser() {
        final String id = "my_id";
        final String username = "my_username";
        final String ipAddress = "11.22.33.44";
        final String email = "test@company.com";
        final String someString = "my String";
        final UUID uuid = UUID.randomUUID();
        final int number = 25;

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("user.id", Variant.ofString(id))
                        .tag("user.email", Variant.ofString(email))
                        .tag("user.username", Variant.ofString(username))
                        .tag("user.ip_address", Variant.ofString(ipAddress))
                        .tag("user.my_field", Variant.ofString(someString))
                        .tag("user.int", Variant.ofInteger(number))
                        .tag("user.UUID", Variant.ofUuid(uuid))
                        .tag("user.null", Variant.ofNull())
                        .build()
                ))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        UserInterface userInterface = (UserInterface) sentryEvent.getSentryInterfaces().get("sentry.interfaces.User");
        Assert.assertEquals(id, userInterface.getId());
        Assert.assertEquals(username, userInterface.getUsername());
        Assert.assertEquals(ipAddress, userInterface.getIpAddress());
        Assert.assertEquals(email, userInterface.getEmail());
        Assert.assertEquals(someString, userInterface.getData().get("my_field"));
        Assert.assertEquals(uuid, userInterface.getData().get("UUID"));
        Assert.assertEquals("null", userInterface.getData().get("null"));
    }

    @Test
    public void shouldSetContext() {
        final String name = "My Browser";
        final String version = "79.3.150";
        final String rawDescription = ".NET Framework 4.7.3163.0";
        final boolean rooted = true;
        final String someString = "my String";
        final UUID uuid = UUID.randomUUID();
        final int number = 25;

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("browser.name", Variant.ofString(name))
                        .tag("browser.version", Variant.ofString(version))
                        .tag("os.raw_description", Variant.ofString(rawDescription))
                        .tag("os.rooted", Variant.ofFlag(rooted))
                        .tag("os.my_field", Variant.ofString(someString))
                        .tag("os.int", Variant.ofInteger(number))
                        .tag("os.UUID", Variant.ofUuid(uuid))
                        .tag("os.null", Variant.ofNull())
                        .build()
                ))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Map<String, Map<String, Object>> contexts = sentryEvent.getContexts();
        Assert.assertEquals(name, contexts.get("browser").get("name"));
        Assert.assertEquals(version, contexts.get("browser").get("version"));
        Assert.assertEquals(rawDescription, contexts.get("os").get("raw_description"));
        Assert.assertTrue((boolean) contexts.get("os").get("rooted"));
        Assert.assertEquals(someString, contexts.get("os").get("my_field"));
        Assert.assertEquals(uuid, contexts.get("os").get("UUID"));
        Assert.assertEquals("null", contexts.get("os").get("null"));
    }

    @Test
    public void shouldSetSentryTags() {
        final String stringValue = "My string";
        final long longValue = 100500100500L;
        final boolean booleanValue = true;
        final UUID uuid = UUID.randomUUID();

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("my_string_tag", Variant.ofString(stringValue))
                        .tag("my_long_tag", Variant.ofLong(longValue))
                        .tag("my_boolean_tag", Variant.ofFlag(booleanValue))
                        .tag("my_UUID_tag", Variant.ofUuid(uuid))
                        .tag("my_null_tag", Variant.ofNull())
                        .build()
                ))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertEquals(stringValue, sentryEvent.getTags().get("my_string_tag"));
        Assert.assertEquals(String.valueOf(longValue), sentryEvent.getTags().get("my_long_tag"));
        Assert.assertEquals(String.valueOf(booleanValue), sentryEvent.getTags().get("my_boolean_tag"));
        Assert.assertEquals(String.valueOf(uuid), sentryEvent.getTags().get("my_UUID_tag"));
        Assert.assertEquals("null", sentryEvent.getTags().get("my_null_tag"));
    }

    @Test
    public void shouldSanitizeSentryTagValue() {
        final String stringValue = "  The source string is longer than 200 characters\n" +
                "and contains line feed characters and whitespaces at the beginning and end.\n" +
                "123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_ ";

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("my_string_tag", Variant.ofString(stringValue))
                        .build()
                ))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        final String expectedStringValue = "The source string is longer than 200 characters " +
                "and contains line feed characters and whitespaces at the beginning and end. " +
                "123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456";
        Assert.assertEquals(expectedStringValue, sentryEvent.getTags().get("my_string_tag"));
        Assert.assertEquals(200, sentryEvent.getTags().get("my_string_tag").length());
    }

    @Test
    public void shouldSanitizeEmptySentryTagValue() {
        final String emptyString = "";
        final String whitespaces = "   ";

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("my_string_tag_1", Variant.ofString(emptyString))
                        .tag("my_string_tag_2", Variant.ofString(whitespaces))
                        .build()
                ))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);
        Assert.assertEquals("[empty]", sentryEvent.getTags().get("my_string_tag_1"));
        Assert.assertEquals("[empty]", sentryEvent.getTags().get("my_string_tag_2"));
    }

    @Test
    public void shouldSetExtraFromContainer() {
        final String stringValue = "My string";

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of("my_extra", Variant.ofContainer(
                                Container.of("my_string", Variant.ofString(stringValue))))))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertEquals(
                stringValue,
                ((Map) sentryEvent.getExtra().get("my_extra")).get("my_string")
        );
    }

    @Test
    public void shouldSetExtraFromVector() {
        final String stringValue1 = "one";
        final String stringValue2 = "two";

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of("my_extra", Variant.ofVector(Vector.ofStrings(stringValue1, stringValue2)))))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertTrue(((List) sentryEvent.getExtra().get("my_extra")).contains(stringValue1));
        Assert.assertTrue(((List) sentryEvent.getExtra().get("my_extra")).contains(stringValue2));
    }

    @Test
    public void shouldSetExtraFromVectorWithPrimitives() {
        final byte byteValue = 0;
        final short shortValue = 1;
        final int intValue = 2;
        final long longValue = 3L;
        final boolean flagValue = true;
        final float floatValue = 5.0F;
        final double doubleValue = 6.0;

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("byte_vector", Variant.ofVector(Vector.ofBytes(byteValue)))
                        .tag("short_vector", Variant.ofVector(Vector.ofShorts(shortValue)))
                        .tag("int_vector", Variant.ofVector(Vector.ofIntegers(intValue)))
                        .tag("long_vector", Variant.ofVector(Vector.ofLongs(longValue)))
                        .tag("flag_vector", Variant.ofVector(Vector.ofFlags(flagValue)))
                        .tag("float_vector", Variant.ofVector(Vector.ofFloats(floatValue)))
                        .tag("double_vector", Variant.ofVector(Vector.ofDoubles(doubleValue)))
                        .build()
                ))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertTrue(((List) sentryEvent.getExtra().get("byte_vector")).contains(byteValue));
        Assert.assertTrue(((List) sentryEvent.getExtra().get("short_vector")).contains(shortValue));
        Assert.assertTrue(((List) sentryEvent.getExtra().get("int_vector")).contains(intValue));
        Assert.assertTrue(((List) sentryEvent.getExtra().get("long_vector")).contains(longValue));
        Assert.assertTrue(((List) sentryEvent.getExtra().get("flag_vector")).contains(flagValue));
        Assert.assertTrue(((List) sentryEvent.getExtra().get("float_vector")).contains(floatValue));
        Assert.assertTrue(((List) sentryEvent.getExtra().get("double_vector")).contains(doubleValue));
    }

    @Test
    public void shouldSetUuidConvertedToString() {
        final UUID uuid = UUID.randomUUID();

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.TRACE_ID_TAG.getName(), Variant.ofUuid(uuid))))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertEquals(uuid.toString(), sentryEvent.getTransaction());
    }

    @Test
    public void shouldAcceptStringAsFingerprint() {
        final String fingerprint = "some_fingerprint";

        final ru.kontur.vostok.hercules.protocol.Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.FINGERPRINT_TAG.getName(), Variant.ofString(fingerprint))))
                .build();

        final Event sentryEvent = SENTRY_EVENT_CONVERTER.convert(event);

        Assert.assertEquals(fingerprint, sentryEvent.getFingerprint().get(0));
        Assert.assertEquals(1, sentryEvent.getFingerprint().size());
    }
}
