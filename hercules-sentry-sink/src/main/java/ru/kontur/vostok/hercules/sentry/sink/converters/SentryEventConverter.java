package ru.kontur.vostok.hercules.sentry.sink.converters;

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
import ru.kontur.vostok.hercules.util.Lazy;
import ru.kontur.vostok.hercules.util.application.ApplicationContextHolder;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Convert hercules event to sentry event builder
 */
public class SentryEventConverter {

    private static final Lazy<Sdk> SDK = new Lazy<>(() -> new Sdk(
            "hercules-sentry-sink",
            ApplicationContextHolder.get().getVersion(),
            null
    ));

    private static final Set<String> IGNORED_TAGS = Stream.of(
            CommonTags.ENVIRONMENT_TAG
    ).map(TagDescription::getName).collect(Collectors.toSet());

    private static final String DEFAULT_PLATFORM = "";

    public static io.sentry.event.Event convert(Event logEvent) {

        EventBuilder eventBuilder = new EventBuilder(logEvent.getUuid());
        eventBuilder.withTimestamp(Date.from(TimeUtil.unixTicksToInstant(logEvent.getTimestamp())));

        ContainerUtil.extract(logEvent.getPayload(), LogEventTags.MESSAGE_TAG)
            .ifPresent(eventBuilder::withMessage);

        ContainerUtil.extract(logEvent.getPayload(), LogEventTags.LEVEL_TAG)
            .flatMap(SentryLevelEnumParser::parse)
            .ifPresent(eventBuilder::withLevel);

        ContainerUtil.extract(logEvent.getPayload(), CommonTags.PROPERTIES_TAG).ifPresent(properties -> {
            ContainerUtil.extract(properties, CommonTags.ENVIRONMENT_TAG)
                .ifPresent(eventBuilder::withEnvironment);

            for (Map.Entry<String, Variant> entry : properties) {
                String key = entry.getKey();
                if (!IGNORED_TAGS.contains(key)) {
                    VariantUtil.extractPrimitiveAsString(entry.getValue()).ifPresent(value -> eventBuilder.withTag(key, value));
                }
            }
        });

        ContainerUtil.extract(logEvent.getPayload(), LogEventTags.EXCEPTION_TAG).ifPresent(exception -> {
            final ExceptionInterface exceptionInterface = convertException(exception);
            eventBuilder.withSentryInterface(exceptionInterface);
            eventBuilder.withPlatform(SentryEventConverter.extractPlatform(exceptionInterface));
        });

        io.sentry.event.Event sentryEvent = eventBuilder.build();
        sentryEvent.setSdk(SDK.get());

        return sentryEvent;
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

        if (fileName.endsWith(".java")) {
            return Optional.of("java");
        } else if (fileName.endsWith(".cs")) {
            return Optional.of("csharp");
        } else if (fileName.endsWith(".py")) {
            return Optional.of("python");
        } else {
            return Optional.empty();
        }
    }
}
