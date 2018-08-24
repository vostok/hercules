package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.Event.Level;
import io.sentry.event.EventBuilder;
import io.sentry.event.Sdk;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.tags.RootTags;
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

    private static final FieldDescription EXCEPTIONS_TAG = FieldDescription.create("exc", Type.CONTAINER_VECTOR);
    private static final FieldDescription MESSAGE_TAG = FieldDescription.create("msg", Type.TEXT);
    private static final FieldDescription LEVEL_TAG = FieldDescription.create("lvl", Type.STRING);
    private static final FieldDescription RELEASE_TAG = FieldDescription.create("rlz", Type.STRING);
    private static final FieldDescription SERVER_TAG = FieldDescription.create("srv", Type.STRING);

    private static final Set<String> IGNORED_TAGS = Stream.of(
            SentrySyncProcessor.SENTRY_PROJECT_NAME_TAG,
            EXCEPTIONS_TAG,
            MESSAGE_TAG,
            LEVEL_TAG,
            RootTags.ENVIRONMENT_TAG,
            RELEASE_TAG,
            SERVER_TAG
    ).map(FieldDescription::getName).collect(Collectors.toSet());

    private static final String DEFAULT_PLATFORM = "";

    public static io.sentry.event.Event convert(Event event) {

        EventBuilder eventBuilder = new EventBuilder(event.getId());
        eventBuilder.withTimestamp(Date.from(TimeUtil.gregorianTicksToInstant(event.getId().timestamp())));


        ContainerUtil.<Container[]>extractOptional(event.getPayload(), EXCEPTIONS_TAG)
                .ifPresent(exceptions -> eventBuilder.withSentryInterface(convertExceptions(exceptions)));

        ContainerUtil.<String>extractOptional(event.getPayload(), MESSAGE_TAG)
                .ifPresent(eventBuilder::withMessage);

        ContainerUtil.<String>extractOptional(event.getPayload(), LEVEL_TAG)
                .flatMap(s -> EnumUtil.parseOptional(Level.class, s))
                .ifPresent(eventBuilder::withLevel);

        ContainerUtil.<String>extractOptional(event.getPayload(), RootTags.ENVIRONMENT_TAG)
                .ifPresent(eventBuilder::withEnvironment);

        ContainerUtil.<String>extractOptional(event.getPayload(), RELEASE_TAG)
                .ifPresent(eventBuilder::withRelease);

        ContainerUtil.<String>extractOptional(event.getPayload(), SERVER_TAG)
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
        Optional<Container[]> containers = ContainerUtil.extractOptional(event.getPayload(), EXCEPTIONS_TAG);
        if (!containers.isPresent()) {
            return DEFAULT_PLATFORM;
        }

        return Arrays.stream(containers.get())
                .flatMap(container -> Arrays.stream(ContainerUtil.<Container[]>extractOptional(container, SentryExceptionConverter.STACKTRACE_FIELD).orElse(new Container[0])))
                .findAny()
                .map(container -> ContainerUtil.<String>extractRequired(container, SentryStackTraceElementConverter.FILENAME_FIELD))
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
