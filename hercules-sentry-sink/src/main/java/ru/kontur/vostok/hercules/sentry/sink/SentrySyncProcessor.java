package ru.kontur.vostok.hercules.sentry.sink;

import io.sentry.SentryClient;
import io.sentry.event.Event.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kontur.vostok.hercules.kafka.util.processing.BackendServiceFailedException;
import ru.kontur.vostok.hercules.kafka.util.processing.single.SingleSender;
import ru.kontur.vostok.hercules.protocol.Event;
import ru.kontur.vostok.hercules.protocol.util.ContainerUtil;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryEventConverter;
import ru.kontur.vostok.hercules.sentry.sink.converters.SentryLevelEnumParser;
import ru.kontur.vostok.hercules.tags.CommonTags;
import ru.kontur.vostok.hercules.tags.StackTraceTags;
import ru.kontur.vostok.hercules.util.properties.PropertyDescription;
import ru.kontur.vostok.hercules.util.properties.PropertyDescriptions;

import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * @author Gregory Koshelev
 */
public class SentrySyncProcessor implements SingleSender<UUID, Event> {

    private static class Props {
        static final PropertyDescription<Level> REQUIRED_LEVEL = PropertyDescriptions
                .propertyOfType(Level.class, "sentry.level")
                .withParser(SentryLevelEnumParser::parseAsResult)
                .withDefaultValue(Level.WARNING)
                .build();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SentrySyncProcessor.class);

    private final Level requiredLevel;
    private final SentryClientHolder sentryClientHolder;
    private final SentryProjectRegistry sentryProjectRegistry;

    public SentrySyncProcessor(
            Properties properties,
            SentryClientHolder sentryClientHolder,
            SentryProjectRegistry sentryProjectRegistry
    ) {
        this.requiredLevel = Props.REQUIRED_LEVEL.extract(properties);
        this.sentryClientHolder = sentryClientHolder;
        this.sentryProjectRegistry = sentryProjectRegistry;
    }

    @Override
    public boolean process(UUID key, Event event) throws BackendServiceFailedException {
        Optional<String> project = ContainerUtil.extract(event.getPayload(), CommonTags.PROJECT_TAG);
        if (!project.isPresent()) {
            LOGGER.warn("Missing required tag '{}'", CommonTags.PROJECT_TAG.getName());
            return false;
        }

        Optional<Level> level = ContainerUtil.extract(event.getPayload(), StackTraceTags.LEVEL_TAG)
                .flatMap(SentryLevelEnumParser::parse);
        if (!level.isPresent() || requiredLevel.compareTo(level.get()) < 0) {
            return false;
        }

        Optional<String> sentryProjectName = sentryProjectRegistry.getSentryProjectName(project.get());
        if (!sentryProjectName.isPresent()) {
            LOGGER.warn("Project '{}' not found in registry", project.get());
            return false;
        }

        Optional<SentryClient> sentryClient = sentryClientHolder.getClient(sentryProjectName.get());
        if (!sentryClient.isPresent()) {
            LOGGER.warn("Missing client for project '{}'", project.get());
            return false;
        }

        try {
            io.sentry.event.Event sentryEvent = SentryEventConverter.convert(event);
            sentryClient.get().sendEvent(sentryEvent);
            return true;
        } catch (Exception e) {
            throw new BackendServiceFailedException(e);
        }
    }

    @Override
    public void close() throws Exception {
    }
}
