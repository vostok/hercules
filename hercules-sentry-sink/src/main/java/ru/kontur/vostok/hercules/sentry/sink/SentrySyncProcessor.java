package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import io.sentry.event.Event.Level;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryEventConverter;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryLevelEnumParser;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.StackTraceTags;
import ru.kontur.vostok.hercules.util.logging.LoggingConstants;
import ru.kontur.vostok.hercules.util.properties.PropertiesExtractor;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor extends AbstractProcessor<UUID, Event> {

    private static final Level DEFAULT_REQUIRED_LEVEL = Level.WARNING;

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySyncProcessor.class);

    private static final Logger RECEIVED_EVENTS_LOGGER = LoggerFactory.getLogger(LoggingConstants.RECEIVED_EVENT_LOGGER_NAME);
    private static final Logger PROCESSED_EVENTS_LOGGER = LoggerFactory.getLogger(LoggingConstants.PROCESSED_EVENT_LOGGER_NAME);
    private static final Logger DROPPED_EVENTS_LOGGER = LoggerFactory.getLogger(LoggingConstants.DROPPED_EVENT_LOGGER_NAME);

    private final Level requiredLevel;
    private final SentryClientHolder sentryClientHolder;
    private final SentryProjectRegistry sentryProjectRegistry;

    public SentrySyncProcessor(
            Properties properties,
            SentryClientHolder sentryClientHolder,
            SentryProjectRegistry sentryProjectRegistry
    ) {
        this.requiredLevel = PropertiesExtractor.getAs(properties, "sentry.level", String.class)
                .flatMap(SentryLevelEnumParser::parse)
                .orElse(DEFAULT_REQUIRED_LEVEL);
        this.sentryClientHolder = sentryClientHolder;
        this.sentryProjectRegistry = sentryProjectRegistry;
    }

    @Override
    public void process(UUID key, Event event) {
        RECEIVED_EVENTS_LOGGER.trace("{}", event.getId());

        Optional<String> project = ContainerUtil.extract(event.getPayload(), CommonTags.PROJECT_TAG);
        if (!project.isPresent()) {
            LOGGER.warn("Missing required tag '{}'", CommonTags.PROJECT_TAG.getName());
            DROPPED_EVENTS_LOGGER.trace("{}", event.getId());
            return;
        }

        Optional<Level> level = ContainerUtil.extract(event.getPayload(), StackTraceTags.LEVEL_TAG)
                .flatMap(SentryLevelEnumParser::parse);
        if (!level.isPresent() || requiredLevel.compareTo(level.get()) < 0) {
            DROPPED_EVENTS_LOGGER.trace("{}", event.getId());
            return;
        }

        Optional<String> sentryProjectName = sentryProjectRegistry.getSentryProjectName(project.get());
        if (!sentryProjectName.isPresent()) {
            LOGGER.warn("Project '{}' not found in registry", project.get());
            DROPPED_EVENTS_LOGGER.trace("{}", event.getId());
            return;
        }

        Optional<SentryClient> sentryClient = sentryClientHolder.getClient(sentryProjectName.get());
        if (!sentryClient.isPresent()) {
            LOGGER.warn("Missing client for project '{}'", project.get());
            DROPPED_EVENTS_LOGGER.trace("{}", event.getId());
            return;
        }

        try {
            io.sentry.event.Event sentryEvent = SentryEventConverter.convert(event);
            sentryClient.get().sendEvent(sentryEvent);
            PROCESSED_EVENTS_LOGGER.trace("{}", event.getId());
        }
        catch (Exception e) {
            LOGGER.error("Exception while trying to process event", e);
        }
    }
}
