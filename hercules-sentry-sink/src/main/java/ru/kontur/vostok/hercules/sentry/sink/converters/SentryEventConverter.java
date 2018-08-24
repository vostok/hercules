package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.Event.Level;
import io.sentry.event.EventBuilder;
import io.sentry.event.Sdk;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.fields.CommonFields;
import ru.kontur.vostok.hercules.protocol.fields.StackTraceFields;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.FieldDescription;
import ru.kontur.vostok.hercules.protocol.util.VariantUtil;
import ru.kontur.vostok.hercules.sentry.sink.SentrySyncProcessor;
import ru.kontur.vostok.hercules.util.enumeration.EnumUtil;
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

    private static final String SDK_NAME = "hercules-sentry-sink";

    // TODO: Extract actual version
    private static final String SDK_VERSION = "UNKNOWN";
    private static final Sdk SDK = new Sdk(SDK_NAME, SDK_VERSION, null);

    private static final Set<String> IGNORED_TAGS = Stream.of(
            SentrySyncProcessor.SENTRY_PROJECT_NAME_TAG,
            StackTraceFields.EXCEPTIONS_FIELD,
            StackTraceFields.MESSAGE_FIELD,
            StackTraceFields.LEVEL_FIELD,
            CommonFields.ENVIRONMENT_FIELD,
            StackTraceFields.RELEASE_FIELD,
            StackTraceFields.SERVER_FIELD
    ).map(FieldDescription::getName).collect(Collectors.toSet());

    private static final String DEFAULT_PLATFORM = "";

    public static io.sentry.event.Event convert(Event event) {

        EventBuilder eventBuilder = new EventBuilder(event.getId());
        eventBuilder.withTimestamp(Date.from(TimeUtil.gregorianTicksToInstant(event.getId().timestamp())));


        ContainerUtil.<Container[]>extractOptional(event.getPayload(), StackTraceFields.EXCEPTIONS_FIELD)
                .ifPresent(exceptions -> eventBuilder.withSentryInterface(convertExceptions(exceptions)));

        ContainerUtil.<String>extractOptional(event.getPayload(), StackTraceFields.MESSAGE_FIELD)
                .ifPresent(eventBuilder::withMessage);

        ContainerUtil.<String>extractOptional(event.getPayload(), StackTraceFields.LEVEL_FIELD)
                .flatMap(s -> EnumUtil.parseOptional(Level.class, s))
                .ifPresent(eventBuilder::withLevel);

        ContainerUtil.<String>extractOptional(event.getPayload(), CommonFields.ENVIRONMENT_FIELD)
                .ifPresent(eventBuilder::withEnvironment);

        ContainerUtil.<String>extractOptional(event.getPayload(), StackTraceFields.RELEASE_FIELD)
                .ifPresent(eventBuilder::withRelease);

        ContainerUtil.<String>extractOptional(event.getPayload(), StackTraceFields.SERVER_FIELD)
                .ifPresent(eventBuilder::withServerName);

        for (Map.Entry<String, Variant> entry : event.getPayload()) {
            String key = entry.getKey();
            if (!IGNORED_TAGS.contains(key)) {
                VariantUtil.extractPrimitiveAsString(entry.getValue()).ifPresent(value -> eventBuilder.withTag(key, value));
            }
        }

        eventBuilder.withPlatform(SentryEventConverter.extractPlatform(event));

        io.sentry.event.Event sentryEvent = eventBuilder.build();
        sentryEvent.setSdk(SDK);

        return sentryEvent;
    }

    private static ExceptionInterface convertExceptions(Container[] exceptions) {
        LinkedList<SentryException> sentryExceptions = Arrays.stream(exceptions)
                .map(SentryExceptionConverter::convert)
                .collect(Collectors.toCollection(LinkedList::new));

        return new ExceptionInterface(sentryExceptions);
    }

    private static String extractPlatform(Event event) {
        Optional<Container[]> containers = ContainerUtil.extractOptional(event.getPayload(), StackTraceFields.EXCEPTIONS_FIELD);
        if (!containers.isPresent()) {
            return DEFAULT_PLATFORM;
        }

        return Arrays.stream(containers.get())
                .flatMap(container -> Arrays.stream(ContainerUtil.<Container[]>extractOptional(container, StackTraceFields.STACKTRACE_FIELD).orElse(new Container[0])))
                .findAny()
                .map(container -> ContainerUtil.<String>extractRequired(container, StackTraceFields.FILENAME_FIELD))
                .map(SentryEventConverter::resolvePlatformByFileName)
                .orElse(DEFAULT_PLATFORM);
    }

    private static String resolvePlatformByFileName(String fileName) {
        if (Objects.isNull(fileName)) {
            return DEFAULT_PLATFORM;
        }

        if (fileName.endsWith(".java")) {
            return "java";
        } else if (fileName.endsWith(".cs")) {
            return "csharp";
        } else if (fileName.endsWith(".py")) {
            return "python";
        }
        return DEFAULT_PLATFORM;
    }
}
