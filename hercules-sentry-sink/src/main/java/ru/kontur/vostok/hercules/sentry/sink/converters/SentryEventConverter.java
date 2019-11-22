package ru.kontur.vostok.hercules.sentry.sink.converters;

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
import ru.kontur.vostok.hercules.protocol.Vector;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.VariantUtil;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.tags.SentryTags;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convert Hercules event to Sentry event builder
 */
public class SentryEventConverter {
    private static final Set<TinyString> STANDARD_PROPERTIES = Stream.of(
            CommonTags.ENVIRONMENT_TAG,
            SentryTags.RELEASE_TAG,
            SentryTags.TRACE_ID_TAG,
            SentryTags.FINGERPRINT_TAG,
            SentryTags.PLATFORM_TAG,
            SentryTags.LOGGER_TAG)
            .map(TagDescription::getName).collect(Collectors.toSet());

    private static final Set<String> STANDARD_CONTEXTS = Stream.of(
            "os",
            "browser",
            "runtime",
            "device",
            "app",
            "gpu")
            .collect(Collectors.toSet());

    private static final String HIDING_SERVER_NAME = " ";
    private static final String DEFAULT_PLATFORM = "";
    private static final String HIDING_IP_ADDRESS = "0.0.0.0";
    private static final String DELIMITER = ".";
    private static final int MAX_TEG_LENGTH = 200;


    private final Sdk sdk;

    public SentryEventConverter(String version) {
        this.sdk = new Sdk("hercules-sentry-sink", version, null);
    }

    public io.sentry.event.Event convert(Event logEvent) {

        EventBuilder eventBuilder = new EventBuilder(logEvent.getUuid());

        eventBuilder.withTimestamp(Date.from(TimeUtil.unixTicksToInstant(logEvent.getTimestamp())));

        eventBuilder.withServerName(HIDING_SERVER_NAME);

        final Container payload = logEvent.getPayload();

        ContainerUtil.extract(payload, LogEventTags.MESSAGE_TAG)
                .ifPresent(eventBuilder::withMessage);

        ContainerUtil.extract(payload, LogEventTags.LEVEL_TAG)
                .flatMap(SentryLevelEnumParser::parse)
                .ifPresent(eventBuilder::withLevel);

        boolean groupingIsDefined = false;
        Optional<Container> exceptionOptional = ContainerUtil.extract(payload, LogEventTags.EXCEPTION_TAG);
        if (exceptionOptional.isPresent()) {
            final ExceptionInterface exceptionInterface = convertException(exceptionOptional.get());
            eventBuilder.withSentryInterface(exceptionInterface);
            eventBuilder.withPlatform(SentryEventConverter.extractPlatform(exceptionInterface));
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

            ContainerUtil.extract(properties, SentryTags.TRACE_ID_TAG)
                    .ifPresent(eventBuilder::withTransaction);

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

        return sentryEvent;
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

            Optional<String> userFieldOptional = cutOffPrefixIfExists("user", tagName);
            if (userFieldOptional.isPresent()) {
                String userField = userFieldOptional.get();
                if (value.getType() == Type.STRING) {
                    switch (userField) {
                        case "id":
                            userBuilder.setId(extractString(value));
                            break;
                        case "ip_address":
                            userBuilder.setIpAddress(extractString(value));
                            break;
                        case "username":
                            userBuilder.setUsername(extractString(value));
                            break;
                        case "email":
                            userBuilder.setEmail(extractString(value));
                            break;
                        default:
                            otherUserDataMap.put(userField, extractString(value));
                    }
                } else {
                    otherUserDataMap.put(userField, extractObject(value));
                }
                continue;
            }

            boolean valueIsContext = false;
            for (String contextName : STANDARD_CONTEXTS) {
                Optional<String> contextFieldOptional = cutOffPrefixIfExists(contextName, tagName);
                if (contextFieldOptional.isPresent()) {
                    String contextField = contextFieldOptional.get();
                    if (!contexts.containsKey(contextName)) {
                        contexts.put(contextName, new HashMap<>());
                    }
                    contexts.get(contextName).put(contextField, extractObject(value));
                    valueIsContext = true;
                    break;
                }
            }
            if (valueIsContext) {
                continue;
            }

            if (VariantUtil.isPrimitive(value)) {
                String stringValue = extractString(value);
                if (stringValue.length() > MAX_TEG_LENGTH) {
                    stringValue = stringValue.substring(0, MAX_TEG_LENGTH);
                }
                eventBuilder.withTag(tagName, stringValue);
                continue;
            }

            eventBuilder.withExtra(tagName, extractObject(value));
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


    private static Optional<String> cutOffPrefixIfExists(String prefix, String sourceName) {
        final String prefixWithDelimiter = prefix + DELIMITER;
        if (sourceName.length() <= prefixWithDelimiter.length()) {
            return Optional.empty();
        }
        if (!sourceName.substring(0, prefixWithDelimiter.length()).equals(prefixWithDelimiter)) {
            return Optional.empty();
        }
        return Optional.of(sourceName.substring(prefixWithDelimiter.length()));
    }

    private static String extractString(Variant variant) {
        if (variant.getType() == Type.STRING) {
            return new String((byte[]) variant.getValue(), StandardCharsets.UTF_8);
        } else {
            return String.valueOf(variant.getValue());
        }
    }

    private static Object extractObject(Variant variant) {
        switch (variant.getType()) {
            case STRING:
                return new String((byte[]) variant.getValue(), StandardCharsets.UTF_8);
            case CONTAINER:
                Map<String, Object> map = new HashMap<>();
                for (Map.Entry<TinyString, Variant> entry : ((Container) variant.getValue()).tags().entrySet()) {
                    map.put(entry.getKey().toString(), extractObject(entry.getValue()));
                }
                return map;
            case VECTOR:
                Vector vector = (Vector) variant.getValue();
                Object[] objects = (Object[]) vector.getValue();
                List<Object> resultList = new ArrayList<>();
                for (Object object : objects) {
                    resultList.add(extractObject(new Variant(vector.getType(), object)));
                }
                return resultList;
            case NULL:
                return "null";
            default:
                return variant.getValue();
        }
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
                .map(SentryEventConverter::resolvePlatformByFileName)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(DEFAULT_PLATFORM);
    }

    private static Optional<String> resolvePlatformByFileName(final String fileName) {
        if (Objects.isNull(fileName)) {
            return Optional.empty();
        }

        final String lowerCaseFileName = fileName.toLowerCase();

        if (lowerCaseFileName.endsWith(".java")) {
            return Optional.of("java");
        } else if (lowerCaseFileName.endsWith(".cs")) {
            return Optional.of("csharp");
        } else if (lowerCaseFileName.endsWith(".py")) {
            return Optional.of("python");
        } else {
            return Optional.empty();
        }
    }

    private static final Set<String> PLATFORMS = new HashSet<>(Arrays.asList(
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
    ));
}
