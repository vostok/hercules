package ru.kontur.vostok.hercules.sentry.sink.converters;

import com.google.common.collect.Sets;
import io.sentry.event.Event.Level;
import io.sentry.event.EventBuilder;
import io.sentry.event.Sdk;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
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

/**
 * Convert hercules event to sentry event builder
 */
public class SentryEventConverter {

    private static final String SDK_NAME = "hercules-sentry-sink";

    // TODO: Extract actual version
    private static final String SDK_VERSION = "UNKNOWN";
    private static final Sdk SDK = new Sdk(SDK_NAME, SDK_VERSION, null);

    private static final String EXCEPTIONS_TAG = "exc";
    private static final String MESSAGE_TAG = "msg";
    private static final String LEVEL_TAG = "lvl";
    private static final String ENVIRONMENT_TAG = "env";
    private static final String RELEASE_TAG = "rlz";
    private static final String SERVER_TAG = "srv";

    private static final Set<String> IGNORED_TAGS = Sets.newHashSet(
            SentrySyncProcessor.SENTRY_PROJECT_NAME_TAG,
            EXCEPTIONS_TAG,
            MESSAGE_TAG,
            LEVEL_TAG,
            ENVIRONMENT_TAG,
            RELEASE_TAG,
            SERVER_TAG
    );

    private static final String DEFAULT_PLATFORM = "";

    public static io.sentry.event.Event convert(Event event) {

        EventBuilder eventBuilder = new EventBuilder(event.getId());
        eventBuilder.withTimestamp(Date.from(TimeUtil.gregorianTicksToInstant(event.getId().timestamp())));


        EventUtil.<Container[]>extractOptional(event, EXCEPTIONS_TAG, Type.CONTAINER_VECTOR)
                .ifPresent(exceptions -> eventBuilder.withSentryInterface(convertExceptions(exceptions)));

        EventUtil.<String>extractOptional(event, MESSAGE_TAG, Type.TEXT)
                .ifPresent(eventBuilder::withMessage);

        EventUtil.<String>extractOptional(event, LEVEL_TAG, Type.STRING)
                .flatMap(s -> EnumUtil.parseOptional(Level.class, s))
                .ifPresent(eventBuilder::withLevel);

        EventUtil.<String>extractOptional(event, ENVIRONMENT_TAG, Type.STRING)
                .ifPresent(eventBuilder::withEnvironment);

        EventUtil.<String>extractOptional(event, RELEASE_TAG, Type.STRING)
                .ifPresent(eventBuilder::withRelease);

        EventUtil.<String>extractOptional(event, SERVER_TAG, Type.STRING)
                .ifPresent(eventBuilder::withServerName);

        for (Map.Entry<String, Variant> entry : event) {
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
        Optional<Container[]> containers = EventUtil.extractOptional(event, EXCEPTIONS_TAG, Type.CONTAINER_VECTOR);
        if (!containers.isPresent()) {
            return DEFAULT_PLATFORM;
        }

        return Arrays.stream(containers.get())
                .flatMap(container -> Arrays.stream(ContainerUtil.<Container[]>extractOptional(container, SentryExceptionConverter.STACKTRACE_FIELD_NAME, Type.CONTAINER_ARRAY).orElse(new Container[0])))
                .findAny()
                .map(container -> ContainerUtil.<String>extractRequired(container, SentryStackTraceElementConverter.FILENAME_FIELD_NAME, Type.STRING))
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
