package ru.kontur.vostok.hercules.sentry.sink;

import com.codahale.metrics.Meter;
import io.sentry.SentryClient;
import io.sentry.event.Event.Level;
import org.apache.kafka.streams.processor.AbstractProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.health.MetricsCollector;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryEventConverter;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryLevelEnumParser;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.StackTraceTags;
import ru.kontur.vostok.hercules.util.logging.LoggingConstants;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor extends AbstractProcessor<UUID, Event> {

    private static class Props {
        static final PropertyDescription<Level> REQUIRED_LEVEL = PropertyDescriptions
                .propertyOfType(Level.class, "sentry.level")
                .withParser(SentryLevelEnumParser::parseAsResult)
                .withDefaultValue(Level.WARNING)
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySyncProcessor.class);

    private static final Logger RECEIVED_EVENTS_LOGGER = LoggerFactory.getLogger(LoggingConstants.RECEIVED_EVENT_LOGGER_NAME);
    private static final Logger PROCESSED_EVENTS_LOGGER = LoggerFactory.getLogger(LoggingConstants.PROCESSED_EVENT_LOGGER_NAME);
    private static final Logger DROPPED_EVENTS_LOGGER = LoggerFactory.getLogger(LoggingConstants.DROPPED_EVENT_LOGGER_NAME);

    private final Level requiredLevel;
    private final SentryClientHolder sentryClientHolder;
    private final SentryProjectRegistry sentryProjectRegistry;

    private final Meter receivedEventsMeter;
    private final Meter receivedEventsSizeMeter;
    private final Meter processedEventsMeter;
    private final Meter droppedEventsMeter;

    public SentrySyncProcessor(
            Properties properties,
            SentryClientHolder sentryClientHolder,
            SentryProjectRegistry sentryProjectRegistry,
            MetricsCollector metricsCollector
    ) {
        this.requiredLevel = Props.REQUIRED_LEVEL.extract(properties);
        this.sentryClientHolder = sentryClientHolder;
        this.sentryProjectRegistry = sentryProjectRegistry;

        this.receivedEventsMeter = metricsCollector.meter("receivedEvents");
        this.receivedEventsSizeMeter = metricsCollector.meter("receivedEventsSize");
        this.processedEventsMeter = metricsCollector.meter("processedEvents");
        this.droppedEventsMeter = metricsCollector.meter("droppedEvents");
    }

    @Override
    public void process(UUID key, Event event) {
        markReceivedEvent(event);


        Optional<String> project = ContainerUtil.extract(event.getPayload(), CommonTags.PROJECT_TAG);
        if (!project.isPresent()) {
            LOGGER.warn("Missing required tag '{}'", CommonTags.PROJECT_TAG.getName());
            markDroppedEvent(event);
            return;
        }

        Optional<Level> level = ContainerUtil.extract(event.getPayload(), StackTraceTags.LEVEL_TAG)
                .flatMap(SentryLevelEnumParser::parse);
        if (!level.isPresent() || requiredLevel.compareTo(level.get()) < 0) {
            markDroppedEvent(event);
            return;
        }

        Optional<String> sentryProjectName = sentryProjectRegistry.getSentryProjectName(project.get());
        if (!sentryProjectName.isPresent()) {
            LOGGER.warn("Project '{}' not found in registry", project.get());
            markDroppedEvent(event);
            return;
        }

        Optional<SentryClient> sentryClient = sentryClientHolder.getClient(sentryProjectName.get());
        if (!sentryClient.isPresent()) {
            LOGGER.warn("Missing client for project '{}'", project.get());
            markDroppedEvent(event);
            return;
        }

        try {
            io.sentry.event.Event sentryEvent = SentryEventConverter.convert(event);
            sentryClient.get().sendEvent(sentryEvent);
            markProcessedEvent(event);
        }
        catch (Exception e) {
            LOGGER.error("Exception while trying to process event", e);
        }
    }

    private void markReceivedEvent(Event event) {
        if (RECEIVED_EVENTS_LOGGER.isTraceEnabled()) {
            RECEIVED_EVENTS_LOGGER.trace("{}", event.getId());
        }
        receivedEventsMeter.mark();
        receivedEventsSizeMeter.mark(event.getBytes().length);
    }

    private void markDroppedEvent(Event event) {
        if (DROPPED_EVENTS_LOGGER.isTraceEnabled()) {
            DROPPED_EVENTS_LOGGER.trace("{}", event.getId());
        }
        droppedEventsMeter.mark();
    }

    private void markProcessedEvent(Event event) {
        if (PROCESSED_EVENTS_LOGGER.isTraceEnabled()) {
            PROCESSED_EVENTS_LOGGER.trace("{}", event.getId());
        }
        processedEventsMeter.mark();
    }
}
