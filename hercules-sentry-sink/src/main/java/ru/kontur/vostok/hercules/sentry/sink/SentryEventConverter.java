package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.event.EventBuilder;
import io.sentry.event.Sdk;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Type;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.util.time.TimeUtil;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

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
}
