package ru.kontur.vostok.hercules.sentry.client.impl;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.junit.Assert;
import org.junit.Test;
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

/**
 * @author Petr Demenev
 */
public class SentryEventConverterTest {

    private static final String someUuid = "00000000-0000-1000-994f-8fcf383f0000";
    private static final String platformFromStacktrace = "java";

    private static final SentryEventConverterImpl SENTRY_EVENT_CONVERTER = new SentryEventConverterImpl("0.0.0");

    private static Container createException() {
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

    @Test
    public void shouldConvertEventWithMessage() {
        final String message = "This is message sample";
        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.MESSAGE_TAG.getName(), Variant.ofString(message))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertNotNull(sentryEvent.getMessage());
        Assert.assertEquals(message, sentryEvent.getMessage().getMessage());
    }

    @Test
    public void shouldConvertEventWithExceptions() {
        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .build();

        SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertNotNull(sentryEvent.getExceptions());
        SentryException exception = sentryEvent.getExceptions().get(0);

        Assert.assertEquals("Some error of ExceptionClass happened", exception.getValue());
        Assert.assertEquals("com.example.test.exceptions.ExceptionClass", exception.getType());
        Assert.assertEquals("some module", exception.getModule());

        SentryStackTrace stackTrace = exception.getStacktrace();
        Assert.assertNotNull(stackTrace);
        Assert.assertNotNull(stackTrace.getFrames());
        Assert.assertEquals(2, stackTrace.getFrames().size());

        SentryStackFrame stackFrame = stackTrace.getFrames().get(0);
        Assert.assertEquals("com.example.test.SomeModule", stackTrace.getFrames().get(0).getPackage());
        Assert.assertEquals("function", stackFrame.getFunction());
        Assert.assertEquals("SomeModule.java", stackFrame.getFilename());
        Assert.assertEquals(Integer.valueOf(100), stackFrame.getLineno());
        Assert.assertEquals(Integer.valueOf(12), stackFrame.getColno());

        stackFrame = stackTrace.getFrames().get(1);
        Assert.assertEquals("com.example.test.AnotherModule", stackFrame.getPackage());
        Assert.assertEquals("function", stackFrame.getFunction());
        Assert.assertEquals("AnotherModule.java", stackFrame.getFilename());
        Assert.assertEquals(Integer.valueOf(200), stackFrame.getLineno());
        Assert.assertEquals(Integer.valueOf(13), stackFrame.getColno());
    }

    @Test
    public void shouldExtractPlatformValue() {
        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertEquals(platformFromStacktrace, sentryEvent.getPlatform());
    }

    @Test
    public void shouldSetPlatformIfPlatformTagAbsentInPropertiesTag() {
        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.empty()))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

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
        final Event event = EventBuilder
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

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertEquals(traceId, sentryEvent.getTransaction());
        Assert.assertEquals(traceId, sentryEvent.getTags().get("traceId"));
        Assert.assertEquals(release, sentryEvent.getRelease());

        Assert.assertNotNull(sentryEvent.getFingerprint());
        Assert.assertTrue(sentryEvent.getFingerprint().get(0).contains(fingerprintWord1));
        Assert.assertTrue(sentryEvent.getFingerprint().get(1).contains(fingerprintWord2));
        Assert.assertEquals(2, sentryEvent.getFingerprint().size());
        Assert.assertEquals(platform.toLowerCase(), sentryEvent.getPlatform());
        Assert.assertEquals(logger, sentryEvent.getLogger());
    }

    @Test
    public void shouldSetFingerprintByMessageTemplate() {
        final String messageTemplate = "My message template";
        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.MESSAGE_TEMPLATE_TAG.getName(), Variant.ofString(messageTemplate))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertNotNull(sentryEvent.getFingerprint());
        Assert.assertTrue(sentryEvent.getFingerprint().get(0).contains(messageTemplate));
        Assert.assertEquals(1, sentryEvent.getFingerprint().size());
    }

    @Test
    public void shouldNotSetFingerprintByMessageTemplateIfExceptionExists() {
        final String messageTemplate = "My message template";
        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.MESSAGE_TEMPLATE_TAG.getName(), Variant.ofString(messageTemplate))
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertNotNull(sentryEvent.getFingerprint());
        Assert.assertEquals(0, sentryEvent.getFingerprint().size());
    }

    @Test
    public void shouldNotSetFingerprintByMessageTemplateIfItIsSetExplicitly() {
        final String messageTemplate = "My message template";
        final String fingerprint = "my_label";
        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.MESSAGE_TEMPLATE_TAG.getName(), Variant.ofString(messageTemplate))
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.FINGERPRINT_TAG.getName(), Variant.ofVector(Vector.ofStrings(fingerprint)))))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertNotNull(sentryEvent.getFingerprint());
        Assert.assertTrue(sentryEvent.getFingerprint().contains(fingerprint));
        Assert.assertEquals(1, sentryEvent.getFingerprint().size());
    }

    @Test
    public void shouldNotSetUnknownPlatform() {
        final String unknownPlatform = "pascal";
        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(LogEventTags.EXCEPTION_TAG.getName(), Variant.ofContainer(createException()))
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.PLATFORM_TAG.getName(), Variant.ofString(unknownPlatform))))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

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

        final Event event = EventBuilder
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

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        User user= sentryEvent.getUser();
        Assert.assertNotNull(user);
        Assert.assertEquals(id, user.getId());
        Assert.assertEquals(username, user.getUsername());
        Assert.assertEquals(ipAddress, user.getIpAddress());
        Assert.assertEquals(email, user.getEmail());

        Assert.assertNotNull(user.getUnknown());
        Assert.assertEquals(someString, user.getUnknown().get("my_field"));
        Assert.assertEquals(uuid, user.getUnknown().get("UUID"));
        Assert.assertEquals("null", user.getUnknown().get("null"));
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
        final Date date = Date.from(Instant.now());

        final Event event = EventBuilder
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

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        ContextContainer contexts = sentryEvent.getContexts();
        Assert.assertEquals(name, Objects.requireNonNull(contexts.getBrowser()).getName());
        Assert.assertEquals(version, contexts.getBrowser().getVersion());
        Assert.assertEquals(rawDescription, Objects.requireNonNull(contexts.getOs()).getRawDescription());
        Assert.assertEquals(someString, Objects.requireNonNull(
                contexts.getOs().getUnknown()).get("my_field"));
        Assert.assertEquals(uuid, contexts.getOs().getUnknown().get("UUID"));
        Assert.assertEquals(true, contexts.getOs().isRooted());
        Assert.assertEquals("null", contexts.getOs().getUnknown().get("null"));

        Device device = contexts.getDevice();
        Assert.assertNotNull(device);
        Assert.assertEquals(true, device.isCharging());
        Assert.assertEquals(true, device.isOnline());
        Assert.assertNull(device.isSimulator());
        Assert.assertNotNull(device.getUnknown());
        Assert.assertEquals("yes", device.getUnknown().get("simulator"));
        Assert.assertEquals(Long.valueOf(43), device.getMemorySize());
        Assert.assertEquals(Long.valueOf(32), device.getFreeMemory());
        Assert.assertNull(device.getUsableMemory());
        Assert.assertEquals("14K", device.getUnknown().get("usable_memory"));
        Assert.assertEquals(Integer.valueOf(640), device.getScreenWidthPixels());
        Assert.assertNull(device.getScreenHeightPixels());
        Assert.assertEquals(Long.MAX_VALUE, device.getUnknown().get("screen_height_pixels"));
        Assert.assertNull(device.getScreenDpi());
        Assert.assertEquals("300dpi", device.getUnknown().get("screen_dpi"));
        Assert.assertNotNull(contexts.getApp());
        Assert.assertEquals(date.toString(), contexts.getApp().getAppStartTime());
    }

    @Test
    public void shouldSetSentryTags() {
        final String stringValue = "My string";
        final long longValue = 100500100500L;
        final boolean booleanValue = true;
        final UUID uuid = UUID.randomUUID();

        final Event event = EventBuilder
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

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

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

        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("my_string_tag", Variant.ofString(stringValue))
                        .build()
                ))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        final String expectedStringValue = "The source string is longer than 200 characters " +
                "and contains line feed characters and whitespaces at the beginning and end. " +
                "123456789_123456789_123456789_123456789_123456789_123456789_123456789_123456";
        Assert.assertEquals(expectedStringValue, sentryEvent.getTags().get("my_string_tag"));
        Assert.assertEquals(200, Objects.requireNonNull(sentryEvent.getTags().get("my_string_tag")).length());
    }

    @Test
    public void shouldSanitizeEmptySentryTagValue() {
        final String emptyString = "";
        final String whitespaces = "   ";

        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(Container.builder()
                        .tag("my_string_tag_1", Variant.ofString(emptyString))
                        .tag("my_string_tag_2", Variant.ofString(whitespaces))
                        .build()
                ))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();
        Assert.assertEquals("[empty]", sentryEvent.getTags().get("my_string_tag_1"));
        Assert.assertEquals("[empty]", sentryEvent.getTags().get("my_string_tag_2"));
    }

    @Test
    public void shouldSetExtraFromContainer() {
        final String stringValue = "My string";

        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of("my_extra", Variant.ofContainer(
                                Container.of("my_string", Variant.ofString(stringValue))))))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertEquals(
                stringValue,
                ((Map<?,?>) Objects.requireNonNull(sentryEvent.getExtra().get("my_extra"))).get("my_string")
        );
    }

    @Test
    public void shouldSetExtraFromVector() {
        final String stringValue1 = "one";
        final String stringValue2 = "two";

        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of("my_extra", Variant.ofVector(Vector.ofStrings(stringValue1, stringValue2)))))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("my_extra"))).contains(stringValue1));
        Assert.assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("my_extra"))).contains(stringValue2));
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

        final Event event = EventBuilder
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

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertNotNull(sentryEvent.getExtra().get("byte_vector"));

        Assert.assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("byte_vector"))).contains(byteValue));
        Assert.assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("short_vector"))).contains(shortValue));
        Assert.assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("int_vector"))).contains(intValue));
        Assert.assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("long_vector"))).contains(longValue));
        Assert.assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("flag_vector"))).contains(flagValue));
        Assert.assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("float_vector"))).contains(floatValue));
        Assert.assertTrue(((List<?>) Objects.requireNonNull(sentryEvent.getExtra().get("double_vector"))).contains(doubleValue));
    }

    @Test
    public void shouldSetUuidConvertedToString() {
        final UUID uuid = UUID.randomUUID();

        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.TRACE_ID_TAG.getName(), Variant.ofUuid(uuid))))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertEquals(uuid.toString(), sentryEvent.getTransaction());
    }

    @Test
    public void shouldAcceptStringAsFingerprint() {
        final String fingerprint = "some_fingerprint";

        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.of(SentryTags.FINGERPRINT_TAG.getName(), Variant.ofString(fingerprint))))
                .build();

        final SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();

        Assert.assertNotNull(sentryEvent.getFingerprint());
        Assert.assertEquals(fingerprint, sentryEvent.getFingerprint().get(0));
        Assert.assertEquals(1, sentryEvent.getFingerprint().size());
    }

    @Test
    public void shouldSetLevel() {
        String[] values = new String[] {"fatal", "error", "warning", "info", "debug"};
        SentryLevel[] results = new SentryLevel[] {SentryLevel.FATAL, SentryLevel.ERROR, SentryLevel.WARNING, SentryLevel.INFO, SentryLevel.DEBUG};
        for (int i = 0; i < values.length; i++) {
            Event event = EventBuilder
                    .create(0, someUuid)
                    .tag(LogEventTags.LEVEL_TAG.getName(), Variant.ofString(values[i]))
                    .build();
            SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();
            Assert.assertNotNull(sentryEvent.getLevel());
            Assert.assertEquals(results[i], sentryEvent.getLevel());
        }
    }

    @Test
    public void shouldSetTimestamp() {
        long timestamp = TimeUtil.millisToTicks(Instant.now().toEpochMilli());
        final Event event = EventBuilder
                .create(timestamp, someUuid).build();
        SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();
        Assert.assertEquals(Instant.ofEpochMilli(TimeUtil.ticksToMillis(timestamp)), sentryEvent.getTimestamp());
    }

    @Test
    public void shouldSetValuesFromStandardProperties() {
        final Event event = EventBuilder
                .create(0, someUuid)
                .tag(CommonTags.PROPERTIES_TAG.getName(), Variant.ofContainer(
                        Container.builder()
                                .tag(CommonTags.ENVIRONMENT_TAG.getName(), Variant.ofString("fatal"))
                                .build()))
                .build();
        SentryEvent sentryEvent = SENTRY_EVENT_CONVERTER.convert(event).getSentryEvent();
        Assert.assertEquals("fatal", sentryEvent.getEnvironment());
        SdkVersion sdkVersion = sentryEvent.getSdk();
        Assert.assertEquals("hercules-sentry-sink", sdkVersion.getName());
        Assert.assertEquals("0.0.0", sdkVersion.getVersion());
        Assert.assertEquals(someUuid, sentryEvent.getEventId().toString());
    }
}
