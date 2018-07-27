package ru.kontur.vostok.hercules.sentry.sink;

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
import ru.kontur.vostok.hercules.protocol.util.VariantUtil;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Convert hercules event to sentry event builder
 */
public class SentryEventConverter {

    private static final String SDK_NAME = "hercules-sentry-sink";
    private static final String SDK_VERSION = Optional.ofNullable(SentryEventConverter.class.getPackage().getImplementationVersion()).orElse("UNKNOWN");
    private static final Sdk SDK = new Sdk(SDK_NAME, SDK_VERSION, null);

    private static Optional<String> get(Variant variant) {
        if (Objects.isNull(variant) || (variant.getType() != Type.TEXT && variant.getType() != Type.STRING)) {
            return Optional.empty();
        } else {
            return Optional.of(new String((byte[]) variant.getValue(), StandardCharsets.UTF_8));
        }
    }

    public static io.sentry.event.Event convert(Event event) {

        EventBuilder eventBuilder = new EventBuilder(event.getId());
        eventBuilder.withTimestamp(Date.from(TimeUtil.gregorianTicksToInstant(event.getId().timestamp())));

        eventBuilder.withSentryInterface(extractException(event));

        // TODO: Implement transformation of stacktraces
        for (Map.Entry<String, Variant> entry : event) {
            String key = entry.getKey();
            if ("message".equals(key)) {
                get(entry.getValue()).ifPresent(eventBuilder::withMessage);
            }
            else if ("environment".equals(key)) {
                get(entry.getValue()).ifPresent(eventBuilder::withEnvironment);
            }
            else if ("release".equals(key)) {
                get(entry.getValue()).ifPresent(eventBuilder::withRelease);
            }
            else {
                get(entry.getValue()).ifPresent(value -> eventBuilder.withTag(key, value));
            }
        }

        io.sentry.event.Event sentryEvent = eventBuilder.build();
        sentryEvent.setSdk(SDK);

        return sentryEvent;
    }

    private static ExceptionInterface extractException(Event event) {
        Optional<Container[]> exceptions = VariantUtil.extractRegardingType(event.getTag("exceptions"), Type.CONTAINER_VECTOR);
        if (!exceptions.isPresent()) {
            return null;
        }

        LinkedList<SentryException> sentryExceptions = Arrays.stream(exceptions.get())
                .map(SentryEventConverter::convertSingleException)
                .collect(Collectors.toCollection(LinkedList::new));

        return new ExceptionInterface(sentryExceptions);
    }

    private static SentryException convertSingleException(Container exception) {
        String type = ContainerUtil.extractRequired(exception, "type", Type.STRING);
        String value = ContainerUtil.extractRequired(exception, "value", Type.TEXT);
        String module = ContainerUtil.extractRequired(exception, "module", Type.TEXT);
        Container[] stacktrace = ContainerUtil.extractRequired(exception, "stacktrace", Type.CONTAINER_VECTOR);

        return new SentryException(
                type,
                value,
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
                (Integer) ContainerUtil.extractOptional(frame, "colno", Type.SHORT).orElse(null),
                ContainerUtil.extractRequired(frame, "abs_path", Type.TEXT),
                null
        );
    }
}
