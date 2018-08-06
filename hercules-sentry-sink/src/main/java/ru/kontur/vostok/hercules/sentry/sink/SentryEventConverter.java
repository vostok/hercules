package ru.kontur.vostok.hercules.sentry.sink;

import com.google.common.collect.Sets;
import io.sentry.event.Event.Level;
import io.sentry.event.EventBuilder;
import io.sentry.event.Sdk;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import io.sentry.event.interfaces.SentryStackTraceElement;
import io.sentry.event.interfaces.StackTraceInterface;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.EventUtil;
import ru.kontur.vostok.hercules.protocol.util.VariantUtil;
import ru.kontur.vostok.hercules.util.enumeration.EnumUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
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

    private static final String EXCEPTIONS_TAG = "exceptions";
    private static final String MESSAGE_TAG = "message";
    private static final String LEVEL_TAG = "level";
    private static final String PLATFORM_TAG = "platform";
    private static final String ENVIRONMENT_TAG = "environment";
    private static final String RELEASE_TAG = "release";
    private static final String SERVER_TAG = "server";

    private static final Set<String> IGNORED_TAGS = Sets.newHashSet(
            SentrySyncProcessor.SENTRY_PROJECT_NAME_TAG,
            EXCEPTIONS_TAG,
            MESSAGE_TAG,
            LEVEL_TAG,
            PLATFORM_TAG,
            ENVIRONMENT_TAG,
            RELEASE_TAG,
            SERVER_TAG
    );

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

        EventUtil.<String>extractOptional(event, PLATFORM_TAG, Type.STRING)
                .ifPresent(eventBuilder::withPlatform);

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

        io.sentry.event.Event sentryEvent = eventBuilder.build();
        sentryEvent.setSdk(SDK);

        return sentryEvent;
    }

    private static ExceptionInterface convertExceptions(Container[] exceptions) {
        LinkedList<SentryException> sentryExceptions = Arrays.stream(exceptions)
                .map(SentryEventConverter::convertSingleException)
                .collect(Collectors.toCollection(LinkedList::new));

        return new ExceptionInterface(sentryExceptions);
    }

    private static SentryException convertSingleException(Container exception) {
        String type = ContainerUtil.extractRequired(exception, "type", Type.STRING);
        String value = ContainerUtil.extractRequired(exception, "value", Type.TEXT);
        String module = ContainerUtil.extractRequired(exception, "module", Type.TEXT);
        Container[] stacktrace = ContainerUtil.extractRequired(exception, "stacktrace", Type.CONTAINER_ARRAY);

        return new SentryException(
                value,
                type,
                module,
                convertStacktrace(stacktrace)
        );
    }

    private static StackTraceInterface convertStacktrace(Container[] stacktrace) {
        return new StackTraceInterface(Arrays.stream(stacktrace)
                .map(SentryEventConverter::convertSingleFrame)
                .toArray(SentryStackTraceElement[]::new)
        );
    }

    private static SentryStackTraceElement convertSingleFrame(Container frame){
        return new SentryStackTraceElement(
                ContainerUtil.extractRequired(frame, "module", Type.TEXT),
                ContainerUtil.extractRequired(frame, "function", Type.STRING),
                ContainerUtil.extractRequired(frame, "filename", Type.STRING),
                ContainerUtil.extractRequired(frame, "lineno", Type.INTEGER),
                ContainerUtil.<Short>extractOptional(frame, "colno", Type.SHORT).map(Short::intValue).orElse(null),
                ContainerUtil.extractRequired(frame, "abs_path", Type.TEXT),
                null
        );
    }
}
