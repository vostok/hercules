package ru.kontur.vostok.hercules.sentry.sink.converters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.sentry.event.EventBuilder;
import io.sentry.event.Sdk;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import io.sentry.event.interfaces.SentryStackTraceElement;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.ExceptionTags;
import ru.kontur.vostok.hercules.tags.LogEventTags;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.VariantUtil;
import ru.kontur.vostok.hercules.tags.SentryTags;
import ru.kontur.vostok.hercules.util.Lazy;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
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

    private static final Lazy<Sdk> SDK = new Lazy<>(() -> new Sdk(
            "hercules-sentry-sink",
            ApplicationContextHolder.get().getVersion(),
            null
    ));

    private static final Set<String> STANDARD_PROPERTIES = Stream.of(
            CommonTags.ENVIRONMENT_TAG,
            SentryTags.RELEASE_TAG,
            SentryTags.TRACE_ID_TAG,
            SentryTags.FINGERPRINT_TAG,
            SentryTags.PLATFORM_TAG,
            SentryTags.USER_TAG)
            .map(TagDescription::getName).collect(Collectors.toSet());

    private static final String HIDING_SERVER_NAME = " ";
    private static final String DEFAULT_PLATFORM = "";

    public static io.sentry.event.Event convert(Event logEvent) {

        EventBuilder eventBuilder = new EventBuilder(logEvent.getUuid());

        eventBuilder.withTimestamp(Date.from(TimeUtil.unixTicksToInstant(logEvent.getTimestamp())));

        eventBuilder.withServerName(HIDING_SERVER_NAME);

        final Container payload = logEvent.getPayload();
        ContainerUtil.extract(payload, LogEventTags.MESSAGE_TAG)
                .ifPresent(eventBuilder::withMessage);

        ContainerUtil.extract(payload, LogEventTags.LEVEL_TAG)
                .flatMap(SentryLevelEnumParser::parse)
                .ifPresent(eventBuilder::withLevel);

        ContainerUtil.extract(payload, LogEventTags.EXCEPTION_TAG)
                .ifPresent(exception -> {
                    final ExceptionInterface exceptionInterface = convertException(exception);
                    eventBuilder.withSentryInterface(exceptionInterface);
                    eventBuilder.withPlatform(SentryEventConverter.extractPlatform(exceptionInterface));
                });

        ContainerUtil.extract(payload, LogEventTags.STACK_TRACE_TAG)
                .ifPresent(stackTrace -> eventBuilder.withExtra("stackTrace", stackTrace));

        ContainerUtil.extract(payload, CommonTags.PROPERTIES_TAG).ifPresent(properties -> {

            ContainerUtil.extract(properties, CommonTags.ENVIRONMENT_TAG)
                    .ifPresent(eventBuilder::withEnvironment);

            ContainerUtil.extract(properties, SentryTags.RELEASE_TAG)
                    .ifPresent(eventBuilder::withRelease);

            ContainerUtil.extract(properties, SentryTags.TRACE_ID_TAG)
                    .ifPresent(eventBuilder::withTransaction);

            ContainerUtil.extract(properties, SentryTags.FINGERPRINT_TAG)
                    .ifPresent(eventBuilder::withFingerprint);

            ContainerUtil.extract(properties, SentryTags.PLATFORM_TAG)
                    .map(String::toLowerCase)
                    .filter(PLATFORMS::contains)
                    .ifPresent(eventBuilder::withPlatform);

            ContainerUtil.extract(properties, SentryTags.USER_TAG)
                    .ifPresent(user -> eventBuilder.withSentryInterface(SentryUserConverter.convert(user)));

            writeExtraData(eventBuilder, properties);
        });

        io.sentry.event.Event sentryEvent = eventBuilder.build();
        sentryEvent.setSdk(SDK.get());

        return sentryEvent;
    }

    private static void writeExtraData(EventBuilder eventBuilder, final Container properties) {
        for (Map.Entry<String, Variant> entry : properties) {
            String key = entry.getKey();
            if (!STANDARD_PROPERTIES.contains(key)) {
                Optional<String> valueOptional = VariantUtil.extractAsString(entry.getValue());
                if (valueOptional.isPresent()) {
                    eventBuilder.withTag(key, valueOptional.get());
                } else {
                    String value;
                    try {
                        value = (new ObjectMapper()).writeValueAsString(entry.getValue());
                    } catch (JsonProcessingException e) {
                        continue;
                    }
                    eventBuilder.withExtra(key, value);
                }
            }
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
