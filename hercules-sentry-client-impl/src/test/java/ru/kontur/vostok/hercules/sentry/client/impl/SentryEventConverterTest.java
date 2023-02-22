package ru.kontur.vostok.hercules.sentry.client.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.EventBuilder;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.ContextContainer;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.Device;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SdkVersion;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryEvent;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryException;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryLevel;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryStackFrame;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.SentryStackTrace;
import ru.kontur.vostok.hercules.sentry.client.impl.client.v7.model.User;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.tags.SentryTags;
import ru.kontur.vostok.hercules.tags.StackFrameTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Petr Demenev
 */
class SentryEventConverterTest {

    private static final String EVENT_ID = "00000000-0000-1000-994f-8fcf383f0000";
    private static final String EXPECTED_PLATFORM = "java";

    SentryEventConverterImpl converter;

    @BeforeEach
    void prepare() {
        converter = new SentryEventConverterImpl("0.0.0");
    }

    @Test
    void shouldConvertEventWithMessage() {
        var message = "This is message sample";
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(LogEventTags.MESSAGE_TAG.getName(), Variant.ofString(message))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertNotNull(sentryEvent.getMessage());
        assertEquals(message, sentryEvent.getMessage().getMessage());
    }

    @Test
    void shouldAllowStringStackTraces() {
        var stacktrace = "Exception in thread \"main\" java.lang.NullPointerException\n"
                + "        at com.example.myproject.Book.getTitle(Book.java:16)\n"
                + "        at com.example.myproject.Author.getBookTitles(Author.java:25)\n"
                + "        at com.example.myproject.Bootstrap.main(Bootstrap.java:14)";
        var event = EventBuilder.create(0, EVENT_ID)
                .tag(LogEventTags.STACK_TRACE_TAG.getName(), Variant.ofString(stacktrace))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertTrue(sentryEvent.getExtra().containsKey("stackTrace"), "expected filled extra.stackTrace field");
        assertEquals(stacktrace, sentryEvent.getExtra().get("stackTrace"));
    }

    @Test
    void shouldSupportStackTraceAbsence() {
        final Event event = EventBuilder
                .create(0, EVENT_ID)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(
                        Container.builder()
                                .tag(ExceptionTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.exceptions.ExceptionClass"))
                                .tag(ExceptionTags.MESSAGE_TAG.getName(), Variant.ofString("Some error of ExceptionClass happened"))
                                .tag(ExceptionTags.MODULE_TAG.getName(), Variant.ofString("some module"))
                                .build()
                ))
                .build();

        assertDoesNotThrow(() -> converter.convert(event));
    }

    @Test
    void shouldConvertEventWithExceptions() {
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertNotNull(sentryEvent.getExceptions());
        SentryException exception = sentryEvent.getExceptions().get(0);

        assertEquals("Some error of ExceptionClass happened", exception.getValue());
        assertEquals("com.example.test.exceptions.ExceptionClass", exception.getType());
        assertEquals("some module", exception.getModule());

        SentryStackTrace stackTrace = exception.getStacktrace();
        assertNotNull(stackTrace);
        assertNotNull(stackTrace.getFrames());
        assertEquals(2, stackTrace.getFrames().size());

        SentryStackFrame stackFrame = stackTrace.getFrames().get(0);
        assertEquals("com.example.test.SomeModule", stackTrace.getFrames().get(0).getPackage());
        assertEquals("function", stackFrame.getFunction());
        assertEquals("SomeModule.java", stackFrame.getFilename());
        assertEquals(Integer.valueOf(100), stackFrame.getLineno());
        assertEquals(Integer.valueOf(12), stackFrame.getColno());

        stackFrame = stackTrace.getFrames().get(1);
        assertEquals("com.example.test.AnotherModule", stackFrame.getPackage());
        assertEquals("function", stackFrame.getFunction());
        assertEquals("AnotherModule.java", stackFrame.getFilename());
        assertEquals(Integer.valueOf(200), stackFrame.getLineno());
        assertEquals(Integer.valueOf(13), stackFrame.getColno());
    }

    @Test
    void shouldExtractPlatformValue() {
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertEquals(EXPECTED_PLATFORM, sentryEvent.getPlatform());
    }

    @Test
    void shouldSetPlatformIfPlatformTagAbsentInPropertiesTag() {
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.empty()))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertEquals(EXPECTED_PLATFORM, sentryEvent.getPlatform());
    }

    @Test
    void shouldSetAttributes() {
        var traceId = "my_trace_ID";
        var release = "my_release 0.1.0";
        var fingerprintWord1 = "{{ default }}";
        var fingerprintWord2 = "my_label";
        var platform = "Python";
        var logger = "Log4j";
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag(SentryTags.TRACE_ID_TAG.getName(), Variant.ofString(traceId))
                        .tag(SentryTags.RELEASE_TAG.getName(), Variant.ofString(release))
                        .tag(SentryTags.FINGERPRINT_TAG.getName(), Variant.ofVector(Vector.ofStrings(fingerprintWord1, fingerprintWord2)))
                        .tag(SentryTags.PLATFORM_TAG.getName(), Variant.ofString(platform))
                        .tag(SentryTags.LOGGER_TAG.getName(), Variant.ofString(logger))
                        .build()
                ))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertEquals(traceId, sentryEvent.getTransaction());
        assertEquals(traceId, sentryEvent.getTags().get("traceId"));
        assertEquals(release, sentryEvent.getRelease());

        assertNotNull(sentryEvent.getFingerprint());
        assertTrue(sentryEvent.getFingerprint().get(0).contains(fingerprintWord1));
        assertTrue(sentryEvent.getFingerprint().get(1).contains(fingerprintWord2));
        assertEquals(2, sentryEvent.getFingerprint().size());
        assertEquals(platform.toLowerCase(), sentryEvent.getPlatform());
        assertEquals(logger, sentryEvent.getLogger());
    }

    @Test
    void shouldSetFingerprintByMessageTemplate() {
        var messageTemplate = "My message template";
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(LogEventTags.MESSAGE_TEMPLATE_TAG.getName(), Variant.ofString(messageTemplate))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertNotNull(sentryEvent.getFingerprint());
        assertTrue(sentryEvent.getFingerprint().get(0).contains(messageTemplate));
        assertEquals(1, sentryEvent.getFingerprint().size());
    }

    @Test
    void shouldNotSetFingerprintByMessageTemplateIfExceptionExists() {
        var messageTemplate = "My message template";
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(LogEventTags.MESSAGE_TEMPLATE_TAG.getName(), Variant.ofString(messageTemplate))
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertNotNull(sentryEvent.getFingerprint());
        assertEquals(0, sentryEvent.getFingerprint().size());
    }

    @Test
    void shouldNotSetFingerprintByMessageTemplateIfItIsSetExplicitly() {
        var messageTemplate = "My message template";
        var fingerprint = "my_label";
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(LogEventTags.MESSAGE_TEMPLATE_TAG.getName(), Variant.ofString(messageTemplate))
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.FINGERPRINT_TAG.getName(), Variant.ofVector(Vector.ofStrings(fingerprint)))))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertNotNull(sentryEvent.getFingerprint());
        assertTrue(sentryEvent.getFingerprint().contains(fingerprint));
        assertEquals(1, sentryEvent.getFingerprint().size());
    }

    @Test
    void shouldNotSetUnknownPlatform() {
        var unknownPlatform = "pascal";
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.PLATFORM_TAG.getName(), Variant.ofString(unknownPlatform))))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertEquals(EXPECTED_PLATFORM, sentryEvent.getPlatform());
    }

    @Test
    void shouldSetUser() {
        String id = "my_id";
        String username = "my_username";
        String ipAddress = "11.22.33.44";
        String email = "test@company.com";
        String someString = "my String";
        UUID uuid = UUID.randomUUID();
        int number = 25;

        var event = EventBuilder
                .create(0, EVENT_ID)
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

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        User user = sentryEvent.getUser();
        assertNotNull(user);
        assertEquals(id, user.getId());
        assertEquals(username, user.getUsername());
        assertEquals(ipAddress, user.getIpAddress());
        assertEquals(email, user.getEmail());

        assertNotNull(user.getUnknown());
        assertEquals(someString, user.getUnknown().get("my_field"));
        assertEquals(uuid, user.getUnknown().get("UUID"));
        assertEquals("null", user.getUnknown().get("null"));
    }

    @Test
    void shouldSetContext() {
        String name = "My Browser";
        String version = "79.3.150";
        String rawDescription = ".NET Framework 4.7.3163.0";
        boolean rooted = true;
        String someString = "my String";
        UUID uuid = UUID.randomUUID();
        int number = 25;
        Date date = Date.from(Instant.now());

        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("browser.name", Variant.ofString(name))
                        .tag("browser.version", Variant.ofString(version))
                        .tag("os.raw_description", Variant.ofString(rawDescription))
                        .tag("os.rooted", Variant.ofFlag(rooted))
                        .tag("os.my_field", Variant.ofString(someString))
                        .tag("os.int", Variant.ofInteger(number))
                        .tag("os.UUID", Variant.ofUuid(uuid))
                        .tag("os.null", Variant.ofNull())
                        .tag("device.charging", Variant.ofString("true"))
                        .tag("device.online", Variant.ofInteger(1))
                        .tag("device.simulator", Variant.ofString("yes"))
                        .tag("device.memory_size", Variant.ofLong(43L))
                        .tag("device.free_memory", Variant.ofInteger(32))
                        .tag("device.usable_memory", Variant.ofString("14K"))
                        .tag("device.screen_width_pixels", Variant.ofInteger(640))
                        .tag("device.screen_height_pixels", Variant.ofLong(Long.MAX_VALUE))
                        .tag("device.screen_dpi", Variant.ofString("300dpi"))
                        .tag("app.app_start_time", Variant.ofString(date.toString()))
                        .build()
                ))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        ContextContainer contexts = sentryEvent.getContexts();
        assertEquals(name, Objects.requireNonNull(contexts.getBrowser()).getName());
        assertEquals(version, contexts.getBrowser().getVersion());
        assertEquals(rawDescription, Objects.requireNonNull(contexts.getOs()).getRawDescription());
        assertEquals(someString, Objects.requireNonNull(
                contexts.getOs().getUnknown()).get("my_field"));
        assertEquals(uuid, contexts.getOs().getUnknown().get("UUID"));
        assertEquals(true, contexts.getOs().isRooted());
        assertEquals("null", contexts.getOs().getUnknown().get("null"));

        Device device = contexts.getDevice();
        assertNotNull(device);
        assertEquals(true, device.isCharging());
        assertEquals(true, device.isOnline());
        assertNull(device.isSimulator());
        assertNotNull(device.getUnknown());
        assertEquals("yes", device.getUnknown().get("simulator"));
        assertEquals(Long.valueOf(43), device.getMemorySize());
        assertEquals(Long.valueOf(32), device.getFreeMemory());
        assertNull(device.getUsableMemory());
        assertEquals("14K", device.getUnknown().get("usable_memory"));
        assertEquals(Integer.valueOf(640), device.getScreenWidthPixels());
        assertNull(device.getScreenHeightPixels());
        assertEquals(Long.MAX_VALUE, device.getUnknown().get("screen_height_pixels"));
        assertNull(device.getScreenDpi());
        assertEquals("300dpi", device.getUnknown().get("screen_dpi"));
        assertNotNull(contexts.getApp());
        assertEquals(date.toString(), contexts.getApp().getAppStartTime());
    }

    @Test
    void shouldSetSentryTags() {
        String stringValue = "My string";
        long longValue = 100500100500L;
        boolean booleanValue = true;
        UUID uuid = UUID.randomUUID();

        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("my_string_tag", Variant.ofString(stringValue))
                        .tag("my_long_tag", Variant.ofLong(longValue))
                        .tag("my_boolean_tag", Variant.ofFlag(booleanValue))
                        .tag("my_UUID_tag", Variant.ofUuid(uuid))
                        .tag("my_null_tag", Variant.ofNull())
                        .build()
                ))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertEquals(stringValue, sentryEvent.getTags().get("my_string_tag"));
        assertEquals(String.valueOf(longValue), sentryEvent.getTags().get("my_long_tag"));
        assertEquals(String.valueOf(booleanValue), sentryEvent.getTags().get("my_boolean_tag"));
        assertEquals(String.valueOf(uuid), sentryEvent.getTags().get("my_UUID_tag"));
        assertEquals("null", sentryEvent.getTags().get("my_null_tag"));
    }

    @Test
    void shouldSanitizeSentryTagValue() {
        var input = "  The source string is longer than 200 characters\n" +
                "and contains line feed characters and whitespaces at the beginning and end.\n" +
                "123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456789_ ";
        var expectedOutput = "The source string is longer than 200 characters " +
                "and contains line feed characters and whitespaces at the beginning and end. " +
                "123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456";

        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("my_string_tag", Variant.ofString(input))
                        .build()
                ))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();
        assertEquals(expectedOutput, sentryEvent.getTags().get("my_string_tag"));
        assertEquals(200, Objects.requireNonNull(sentryEvent.getTags().get("my_string_tag")).length());
    }

    @Test
    void shouldSanitizeEmptySentryTagValue() {
        var emptyString = "";
        var whitespaces = "   ";
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("my_string_tag_1", Variant.ofString(emptyString))
                        .tag("my_string_tag_2", Variant.ofString(whitespaces))
                        .build()
                ))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertEquals("[empty]", sentryEvent.getTags().get("my_string_tag_1"));
        assertEquals("[empty]", sentryEvent.getTags().get("my_string_tag_2"));
    }

    @Test
    void shouldSetExtraFromContainer() {
        var stringValue = "My string";
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of("my_extra", Variant.ofContainer(
                                Container.of("my_string", Variant.ofString(stringValue))))))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertEquals(
                stringValue,
                ((Map<?, ?>) Objects.requireNonNull(sentryEvent.getExtra().get("my_extra"))).get("my_string")
        );
    }

    @Test
    void shouldSetExtraFromVector() {
        var stringValue1 = "one";
        var stringValue2 = "two";
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of("my_extra", Variant.ofVector(Vector.ofStrings(stringValue1, stringValue2)))))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("my_extra"))).contains(stringValue1));
        assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("my_extra"))).contains(stringValue2));
    }

    @Test
    void shouldSetExtraFromVectorWithPrimitives() {
        byte byteValue = 0;
        short shortValue = 1;
        int intValue = 2;
        long longValue = 3L;
        boolean flagValue = true;
        float floatValue = 5.0F;
        double doubleValue = 6.0;
        var event = EventBuilder
                .create(0, EVENT_ID)
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

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertNotNull(sentryEvent.getExtra().get("byte_vector"));
        assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("byte_vector"))).contains(byteValue));
        assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("short_vector"))).contains(shortValue));
        assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("int_vector"))).contains(intValue));
        assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("long_vector"))).contains(longValue));
        assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("flag_vector"))).contains(flagValue));
        assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("float_vector"))).contains(floatValue));
        assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("double_vector"))).contains(doubleValue));
    }

    @Test
    void shouldSetUuidConvertedToString() {
        var uuid = UUID.randomUUID();
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.TRACE_ID_TAG.getName(), Variant.ofUuid(uuid))))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertEquals(uuid.toString(), sentryEvent.getTransaction());
    }

    @Test
    void shouldAcceptStringAsFingerprint() {
        var fingerprint = "some_fingerprint";
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.FINGERPRINT_TAG.getName(), Variant.ofString(fingerprint))))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertNotNull(sentryEvent.getFingerprint());
        assertEquals(fingerprint, sentryEvent.getFingerprint().get(0));
        assertEquals(1, sentryEvent.getFingerprint().size());
    }

    @ParameterizedTest
    @CsvSource({
            "fatal, FATAL",
            "error, ERROR",
            "warning, WARNING",
            "info, INFO",
            "debug, DEBUG"
    })
    void shouldSetLevel(String input, SentryLevel expected) {
        Event event = EventBuilder
                .create(0, EVENT_ID)
                .tag(LogEventTags.LEVEL_TAG.getName(), Variant.ofString(input))
                .build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertNotNull(sentryEvent.getLevel());
        assertEquals(expected, sentryEvent.getLevel());
    }

    @Test
    void shouldSetTimestamp() {
        long timestamp = TimeUtil.millisToTicks(Instant.now().toEpochMilli());
        var event = EventBuilder
                .create(timestamp, EVENT_ID).build();

        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();

        assertEquals(Instant.ofEpochMilli(TimeUtil.ticksToMillis(timestamp)), sentryEvent.getTimestamp());
    }

    @Test
    void shouldSetValuesFromStandardProperties() {
        var event = EventBuilder
                .create(0, EVENT_ID)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.builder()
                                .tag(CommonTags.ENVIRONMENT_TAG.getName(), Variant.ofString("fatal"))
                                .build()))
                .build();
        SentryEvent sentryEvent = converter.convert(event).getSentryEvent();
        assertEquals("fatal", sentryEvent.getEnvironment());
        SdkVersion sdkVersion = sentryEvent.getSdk();
        assertEquals("hercules-sentry-sink", sdkVersion.getName());
        assertEquals("0.0.0", sdkVersion.getVersion());
        assertEquals(EVENT_ID, sentryEvent.getEventId().toString());
    }

    Container createException() {
        return Container.builder()
                .tag(ExceptionTags.TYPE_TAG.getName(), Variant.ofString("com.example.test.exceptions.ExceptionClass"))
                .tag(ExceptionTags.MESSAGE_TAG.getName(), Variant.ofString("Some error of ExceptionClass happened"))
                .tag(ExceptionTags.MODULE_TAG.getName(), Variant.ofString("some module"))
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
}
