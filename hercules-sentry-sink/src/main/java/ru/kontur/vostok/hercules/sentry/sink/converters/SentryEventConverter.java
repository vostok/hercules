package ru.kontur.vostok.hercules.sentry.sink.converters;

import io.sentry.event.Event.Level;
import io.sentry.event.EventBuilder;
import io.sentry.event.Sdk;
import io.sentry.event.interfaces.ExceptionInterface;
import io.sentry.event.interfaces.SentryException;
import ru.kontur.vostok.hercules.protocol.Container;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.Variant;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.StackTraceTags;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.protocol.util.TagDescription;
import ru.kontur.vostok.hercules.protocol.util.VariantUtil;
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
            StackTraceTags.EXCEPTIONS_TAG,
            StackTraceTags.MESSAGE_TAG,
            StackTraceTags.LEVEL_TAG,
            StackTraceTags.RELEASE_TAG,
            StackTraceTags.SERVER_TAG,
            CommonTags.ENVIRONMENT_TAG
    ).map(TagDescription::getName).collect(Collectors.toSet());

    private static final String DEFAULT_PLATFORM = "";

    public static io.sentry.event.Event convert(Event event) {

        EventBuilder eventBuilder = new EventBuilder(event.getId());
        eventBuilder.withTimestamp(Date.from(TimeUtil.gregorianTicksToInstant(event.getId().timestamp())));


        ContainerUtil.extract(event.getPayload(), StackTraceTags.EXCEPTIONS_TAG)
                .ifPresent(exceptions -> eventBuilder.withSentryInterface(convertExceptions(exceptions)));

        ContainerUtil.extract(event.getPayload(), StackTraceTags.MESSAGE_TAG)
                .ifPresent(eventBuilder::withMessage);

        ContainerUtil.extract(event.getPayload(), StackTraceTags.LEVEL_TAG)
                .flatMap(SentryLevelEnumParser::parse)
                .ifPresent(eventBuilder::withLevel);

        ContainerUtil.extract(event.getPayload(), CommonTags.ENVIRONMENT_TAG)
                .ifPresent(eventBuilder::withEnvironment);

        ContainerUtil.extract(event.getPayload(), StackTraceTags.RELEASE_TAG)
                .ifPresent(eventBuilder::withRelease);

        ContainerUtil.extract(event.getPayload(), StackTraceTags.SERVER_TAG)
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
        Optional<Container[]> containers = ContainerUtil.extract(event.getPayload(), StackTraceTags.EXCEPTIONS_TAG);
        if (!containers.isPresent()) {
            return DEFAULT_PLATFORM;
        }

        return Arrays.stream(containers.get())
                .flatMap(container -> Arrays.stream(ContainerUtil.extract(container, StackTraceTags.STACKTRACE_TAG).orElse(new Container[0])))
                .findAny()
                .map(container -> ContainerUtil.extract(container, StackTraceTags.FILENAME_TAG).orElse(null))
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
