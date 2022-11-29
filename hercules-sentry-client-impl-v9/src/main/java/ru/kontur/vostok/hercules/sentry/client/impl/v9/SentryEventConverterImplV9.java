package ru.kontur.vostok.hercules.sentry.client.impl.v9;

import io.sentry.event.Event.Level;
import io.sentry.event.EventBuilder;
import io.sentry.event.Sdk;
import io.sentry.event.User;
import io.sentry.event.UserBuilder;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import io.sentry.event.interfaces.SentryStackTraceElement;
import io.sentry.event.interfaces.UserInterface;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.TinyString;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.sentry.client.SentryConverterUtil;
import ru.kontur.vostok.hercules.sentry.client.SentryEventConverter;
import ru.kontur.vostok.hercules.sentry.client.SentryLevel;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.tags.SentryTags;
import ru.kontur.vostok.hercules.util.parameter.parsing.Parser;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convert Hercules event to Sentry event
 *
 * @author Petr Demenev
 */
public class SentryEventConverterImplV9 implements SentryEventConverter {
    private static final Set<String> PLATFORMS = Set.of(
            "as3",
            "c",
            "cfml",
            "cocoa",
            "csharp",
            "go",
            "java",
            "javascript",
            "native",
            "node",
            "objc",
            "other",
            "perl",
            "php",
            "python",
            "ruby"
    );

    private static final Set<TinyString> STANDARD_PROPERTIES = Stream.of(
            CommonTags.ENVIRONMENT_TAG,
            SentryTags.RELEASE_TAG,
            SentryTags.TRACE_ID_TAG,
            SentryTags.FINGERPRINT_TAG,
            SentryTags.PLATFORM_TAG,
            SentryTags.LOGGER_TAG)
            .map(TagDescription::getName).collect(Collectors.toUnmodifiableSet());

    private static final Set<String> STANDARD_CONTEXTS = Set.of(
            "os",
            "browser",
            "runtime",
            "device",
            "app",
            "gpu");

    private static final String HIDING_SERVER_NAME = " ";
    private static final String DEFAULT_PLATFORM = "";
    private static final String HIDING_IP_ADDRESS = "0.0.0.0";

    private final Parser<SentryLevel> sentryLevelParser;

    private final Sdk sdk;

    public SentryEventConverterImplV9(String herculesVersion) {
        this.sdk = new Sdk("hercules-sentry-sink", herculesVersion, null);
        sentryLevelParser = new SentryLevelParserImplV9();
    }

    @Override
    public SentryEventImplV9 convert(Event logEvent) {

        EventBuilder eventBuilder = new EventBuilder(logEvent.getUuid());

        eventBuilder.withTimestamp(Date.from(TimeUtil.unixTicksToInstant(logEvent.getTimestamp())));

        eventBuilder.withServerName(HIDING_SERVER_NAME);

        final Container payload = logEvent.getPayload();

        ContainerUtil.extract(payload, LogEventTags.MESSAGE_TAG)
                .ifPresent(eventBuilder::withMessage);

        ContainerUtil.extract(payload, LogEventTags.LEVEL_TAG)
                .flatMap(value -> Optional.of(sentryLevelParser.parse(value).get().name()))
                .map(Level::valueOf)
                .ifPresent(eventBuilder::withLevel);

        boolean groupingIsDefined = false;
        Optional<Container> exceptionOptional = ContainerUtil.extract(payload, LogEventTags.EXCEPTION_TAG);
        if (exceptionOptional.isPresent()) {
            final ExceptionInterface exceptionInterface = convertException(exceptionOptional.get());
            eventBuilder.withSentryInterface(exceptionInterface);
            eventBuilder.withPlatform(SentryEventConverterImplV9.extractPlatform(exceptionInterface));
            groupingIsDefined = true;
        }

        ContainerUtil.extract(payload, LogEventTags.STACK_TRACE_TAG)
                .ifPresent(stackTrace -> eventBuilder.withExtra("stackTrace", stackTrace));

        Optional<Container> propertiesOptional = ContainerUtil.extract(payload, CommonTags.PROPERTIES_TAG);
        if (propertiesOptional.isPresent()) {
            Container properties = propertiesOptional.get();

            ContainerUtil.extract(properties, CommonTags.ENVIRONMENT_TAG)
                    .ifPresent(eventBuilder::withEnvironment);

            ContainerUtil.extract(properties, SentryTags.RELEASE_TAG)
                    .ifPresent(eventBuilder::withRelease);

            Optional<String> traceIdOptional = ContainerUtil.extract(properties, SentryTags.TRACE_ID_TAG);
            if (traceIdOptional.isPresent()) {
                eventBuilder.withTransaction(traceIdOptional.get());
                eventBuilder.withTag("traceId", traceIdOptional.get());
            }

            ContainerUtil.extract(properties, SentryTags.PLATFORM_TAG)
                    .map(String::toLowerCase)
                    .filter(PLATFORMS::contains)
                    .ifPresent(eventBuilder::withPlatform);

            ContainerUtil.extract(properties, SentryTags.LOGGER_TAG)
                    .ifPresent(eventBuilder::withLogger);

            Optional<String[]> fingerprintOptional = ContainerUtil.extract(properties, SentryTags.FINGERPRINT_TAG);
            if (fingerprintOptional.isPresent()) {
                eventBuilder.withFingerprint(fingerprintOptional.get());
                groupingIsDefined = true;
            }

            writeOtherData(properties, eventBuilder);
        }

        if (!groupingIsDefined) {
            ContainerUtil.extract(payload, LogEventTags.MESSAGE_TEMPLATE_TAG)
                    .ifPresent(eventBuilder::withFingerprint);
        }

        io.sentry.event.Event sentryEvent = eventBuilder.build();
        sentryEvent.setSdk(sdk);

        return new SentryEventImplV9(sentryEvent);
    }

    private static void writeOtherData(final Container properties, EventBuilder eventBuilder) {
        UserBuilder userBuilder = new UserBuilder();
        Map<String, Object> otherUserDataMap = new HashMap<>();
        Map<String, Map<String, Object>> contexts = new HashMap<>();

        for (Map.Entry<TinyString, Variant> tag : properties.tags().entrySet()) {
            final TinyString tinyString = tag.getKey();
            final String tagName = tinyString.toString();
            final Variant value = tag.getValue();

            if (STANDARD_PROPERTIES.contains(tinyString)) {
                continue;
            }

            Optional<String> userFieldOptional = SentryConverterUtil
                    .cutOffPrefixIfExists("user", tagName);
            if (userFieldOptional.isPresent()) {
                String userField = userFieldOptional.get();
                if (value.getType() == Type.STRING) {
                    switch (userField) {
                        case "id":
                            userBuilder.setId(SentryConverterUtil.extractString(value));
                            break;
                        case "ip_address":
                            userBuilder.setIpAddress(SentryConverterUtil.extractString(value));
                            break;
                        case "username":
                            userBuilder.setUsername(SentryConverterUtil.extractString(value));
                            break;
                        case "email":
                            userBuilder.setEmail(SentryConverterUtil.extractString(value));
                            break;
                        default:
                            otherUserDataMap.put(
                                    userField,
                                    SentryConverterUtil.extractString(value));
                    }
                } else {
                    otherUserDataMap.put(
                            userField,
                            SentryConverterUtil.extractObject(value));
                }
                continue;
            }

            boolean valueIsContext = false;
            for (String contextName : STANDARD_CONTEXTS) {
                Optional<String> contextFieldOptional = SentryConverterUtil
                        .cutOffPrefixIfExists(contextName, tagName);
                if (contextFieldOptional.isPresent()) {
                    String contextField = contextFieldOptional.get();
                    if (!contexts.containsKey(contextName)) {
                        contexts.put(contextName, new HashMap<>());
                    }
                    contexts.get(contextName).put(
                            contextField,
                            SentryConverterUtil.extractObject(value));
                    valueIsContext = true;
                    break;
                }
            }
            if (valueIsContext) {
                continue;
            }

            if (value.getType().isPrimitive()) {
                eventBuilder.withTag(tagName, SentryConverterUtil.sanitizeTagValue(value));
                continue;
            }

            eventBuilder.withExtra(tagName, SentryConverterUtil.extractObject(value));
        }

        User user = userBuilder.setData(otherUserDataMap).build();
        eventBuilder.withSentryInterface(new UserInterface(
                user.getId(),
                user.getUsername(),
                user.getIpAddress() != null ? user.getIpAddress() : HIDING_IP_ADDRESS,
                user.getEmail(),
                user.getData()));
        eventBuilder.withContexts(contexts);
    }

    private static ExceptionInterface convertException(final Container exception) {

        LinkedList<SentryException> sentryExceptions = new LinkedList<>();
        convertException(exception, sentryExceptions);
        return new ExceptionInterface(sentryExceptions);
    }

    private static void convertException(final Container currentException, final LinkedList<SentryException> converted) {
        converted.add(SentryExceptionConverter.convert(currentException));
        ContainerUtil.extract(currentException, ExceptionTags.INNER_EXCEPTIONS_TAG)
                .ifPresent(exceptions -> Arrays.stream(exceptions).forEach(exception -> convertException(exception, converted)));
    }

    private static String extractPlatform(final ExceptionInterface exceptionInterface) {
        return exceptionInterface.getExceptions().stream()
                .flatMap(e -> Arrays.stream(e.getStackTraceInterface().getStackTrace()))
                .map(SentryStackTraceElement::getFileName)
                .map(SentryConverterUtil::resolvePlatformByFileName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(DEFAULT_PLATFORM);
    }
}
